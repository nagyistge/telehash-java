package org.telehash.crypto;

import org.json.JSONObject;
import org.telehash.core.Identity;
import org.telehash.core.LineIdentifier;
import org.telehash.core.OpenPacket;
import org.telehash.core.Telehash;
import org.telehash.core.TelehashException;
import org.telehash.core.UnwrappedOpenPacket;
import org.telehash.network.Path;

public interface CipherSet {

    /**
     * Generate a fresh identity (i.e., RSA public and private key pair) for a
     * newly provisioned Telehash node.
     * 
     * @return The new identity.
     * @throws TelehashException 
     */
    public Identity generateIdentity() throws TelehashException;

    /**
     * Create a new HashNameKeyPair from the provided public and private key.
     * @param privateKey
     * @param publicKey
     * @return The newly created HashNameKeyPair object.
     */
    public HashNameKeyPair createHashNameKeyPair(HashNamePublicKey publicKey, HashNamePrivateKey privateKey);

    /**
     * Decode a hashname public key.
     * 
     * @param buffer The byte buffer containing the encoded key.
     * @return The decoded public key.
     * @throws TelehashException If the buffer cannot be parsed.
     */
    public HashNamePublicKey decodeHashNamePublicKey(byte[] buffer) throws TelehashException;
    
    /**
     * Decode a hashname private key.
     * 
     * @param buffer The byte buffer containing the encoded key.
     * @return The decoded private key.
     * @throws TelehashException If the buffer cannot be parsed.
     */
    public HashNamePrivateKey decodeHashNamePrivateKey(byte[] buffer) throws TelehashException;

    /**
     * Decode a line public key.
     * 
     * @param buffer The byte buffer containing the ANSI X9.63-encoded key.
     * @return The decoded public key.
     * @throws TelehashException If the ANSI X9.63 buffer cannot be parsed.
     */
    public LinePublicKey decodeLinePublicKey(byte[] buffer) throws TelehashException;

    /**
     * Decode a line private key.
     * 
     * @param buffer The byte buffer containing the encoded key.
     * @return The decoded private key.
     * @throws TelehashException If the byte buffer cannot be parsed.
     */
    public LinePrivateKey decodeLinePrivateKey(byte[] buffer) throws TelehashException;

    /**
     * Create a new line key pair from the provided public and private key.
     * @param privateKey
     * @param publicKey
     * @return The newly created key pair.
     */
    public LineKeyPair createLineKeyPair(
            LinePublicKey publicKey,
            LinePrivateKey privateKey
    ) throws TelehashException;
    
    /**
     * Generate a fresh elliptic curve key pair
     */
    public LineKeyPair generateLineKeyPair() throws TelehashException;
    
    public UnwrappedOpenPacket unwrapOpenPacket(
    		HashNamePrivateKey hashNamePrivateKey,
    		byte[] iv,
    		byte[] encryptedSignature,
    		byte[] openParameter,
    		byte[] encryptedInnerPacket,
    		Path path
    ) throws TelehashException;
    
    public OpenPacket verifyOpenPacket(
    		UnwrappedOpenPacket unwrappedOpenPacket,
    		byte[] destination,
    		byte[] lineIdentifierBytes,
    		LineIdentifier lineIdentifier,
    		long openTime,
    		byte[] innerPacketBody
    ) throws TelehashException;
    
    public OpenPacket parseOpenPacket(
            Telehash telehash,
            JSONObject json,
            byte[] body,
            Path path
    ) throws TelehashException;
    
    /**
     * Pre-render an open packet.
     * 
     * @throws TelehashException
     */
    public void preRenderOpenPacket(OpenPacket open) throws TelehashException;
    
    /**
     * Render an open packet into its final form.
     * 
     * This version of the method allows the caller to pass in values for
     * certain otherwise calculated fields, allowing for deterministic open
     * packet creation suitable for unit tests.
     * 
     * @param packet The open packet object.
     * @param iv
     *            The initialization vector to use for this open packet.
     * @param openParameter
     *            The "open" parameter -- the public line key encrypted
     *            with the recipient's hashname public key.
     * @return The rendered open packet as a byte array.
     * @throws TelehashException
     */
    public byte[] renderOpenPacket(
    		OpenPacket packet,
    		Identity identity,
            byte[] iv,
            byte[] openParameter
    ) throws TelehashException;

}
