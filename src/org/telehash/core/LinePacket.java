package org.telehash.core;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.telehash.crypto.Crypto;
import org.telehash.network.Path;

/**
 * A Telehash "line" packet is used to exchange data between two Telehash nodes
 * that have established a shared secret via open packets.
 * 
 * <p>
 * A line packet consists of the following components, in roughly the order in
 * which they should be unpacked:
 * </p>
 * 
 * <ol>
 * <li>The line identifier</li>
 * <li>A random initialization vector (IV) used for the AES encryption of the inner packet.</li>
 * <li>An embedded "inner packet" containing arbitrary data.  This inner packet
 * is AES-CTR encrypted using a key derived from the SHA-256 hash of shared secret,
 * the outgoing line id, and the incoming line id.</li>
 * </ol>
 */
public class LinePacket extends Packet {
    
    private static final String LINE_TYPE = "line";
    
    private static final String LINE_IDENTIFIER_KEY = "line";
    private static final String IV_KEY = "iv";
    
    private static final int IV_SIZE = 16;
    private static final int LINE_IDENTIFIER_SIZE = 16;
    
    static {
        Packet.registerPacketType(LINE_TYPE, LinePacket.class);
    }
    
    private Line mLine;
    private ChannelPacket mChannelPacket;

    public LinePacket(Line line) {
        mLine = line;
        mDestinationNode = line.getRemoteNode();
    }
    
    public LinePacket(Line line, ChannelPacket channelPacket) {
        mLine = line;
        mChannelPacket = channelPacket;
        mDestinationNode = line.getRemoteNode();
    }
    
    // accessor methods
    
    public void setLine(Line line) {
        mLine = line;
    }
    public Line getLine() {
        return mLine;
    }
    
    public void setChannelPacket(ChannelPacket channelPacket) {
        mChannelPacket = channelPacket;
    }
    public ChannelPacket getChannelPacket() {
        return mChannelPacket;
    }
    
    /**
     * Render the open packet into its final form.
     * 
     * @return The rendered open packet as a byte array.
     */
    public byte[] render() throws TelehashException {
        Crypto crypto = mLine.getTelehash().getCrypto();

        // serialize the channel packet
        if (mChannelPacket == null) {
            mChannelPacket = new ChannelPacket();
        }
        byte[] body = mChannelPacket.render();
        
        // generate a random IV
        byte[] iv = crypto.getRandomBytes(IV_SIZE);
        
        // encrypt body
        byte[] encryptedBody = crypto.encryptAES256CTR(body, iv, mLine.getEncryptionKey());
        
        // Form the inner packet containing a current timestamp at, line
        // identifier, recipient hashname, and family (if you have such a
        // value). Your own RSA public key is the packet BODY in the binary DER
        // format.
        byte[] packet;
        try {
            packet = new JSONStringer()
                .object()
                .key(TYPE_KEY)
                .value(LINE_TYPE)
                .key(LINE_IDENTIFIER_KEY)
                .value(mLine.getOutgoingLineIdentifier().asHex())
                .key(IV_KEY)
                .value(Util.bytesToHex(iv))
                .endObject()
                .toString()
                .getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TelehashException(e);
        } catch (JSONException e) {
            throw new TelehashException(e);
        }
        packet = Util.concatenateByteArrays(
                new byte[] {
                        (byte)((packet.length >> 8) & 0xFF),
                        (byte)(packet.length & 0xFF)
                },
                packet,
                encryptedBody
        );

        return packet;
    }
    
    public static LinePacket parse(
            Telehash telehash,
            JSONObject json,
            byte[] body,
            Path path
    ) throws TelehashException {
        Crypto crypto = telehash.getCrypto();
        
        // extract required JSON values
        String ivString = json.getString(IV_KEY);
        assertNotNull(ivString);
        byte[] iv = Util.hexToBytes(ivString);
        assertBufferSize(iv, IV_SIZE);
        String lineIdentifierString = json.getString(LINE_IDENTIFIER_KEY);
        assertNotNull(lineIdentifierString);
        byte[] lineIdentifierBytes = Util.hexToBytes(lineIdentifierString);
        assertBufferSize(lineIdentifierBytes, LINE_IDENTIFIER_SIZE);
        LineIdentifier lineIdentifier = new LineIdentifier(lineIdentifierBytes);
        
        // lookup the line
        Line line = telehash.getSwitch().getLineManager().getLine(lineIdentifier);
        if (line == null) {
            throw new TelehashException("unknown line id: "+Util.bytesToHex(lineIdentifierBytes));
        }

        // decrypt the body
        byte[] decryptedBody = crypto.decryptAES256CTR(body, iv, line.getDecryptionKey());
        
        // parse the embedded channel packet
        ChannelPacket channelPacket = ChannelPacket.parse(telehash, decryptedBody, path);
        
        return new LinePacket(line, channelPacket);
    }
    
    public String toString() {
        String s = "LINE["+mLine+"]";
        if (mSourceNode != null) {
            s += " <"+mSourceNode;
        }
        if (mDestinationNode != null) {
            s += " <"+mDestinationNode;
        }
        return s;
    }


}
