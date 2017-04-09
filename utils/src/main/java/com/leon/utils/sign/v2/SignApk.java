/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leon.utils.sign.v2;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.conscrypt.OpenSSLProvider;

import java.io.Console;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * HISTORICAL NOTE:
 *
 * Prior to the keylimepie release, SignApk ignored the signature
 * algorithm specified in the certificate and always used SHA1withRSA.
 *
 * Starting with JB-MR2, the platform supports SHA256withRSA, so we use
 * the signature algorithm in the certificate to select which to use
 * (SHA256withRSA or SHA1withRSA). Also in JB-MR2, EC keys are supported.
 *
 * Because there are old keys still in use whose certificate actually
 * says "MD5withRSA", we treat these as though they say "SHA1withRSA"
 * for compatibility with older releases.  This can be changed by
 * altering the getAlgorithm() function below.
 */


/**
 * Command line tool to sign JAR files (including APKs and OTA updates) in a way
 * compatible with the mincrypt verifier, using EC or RSA keys and SHA1 or
 * SHA-256 (see historical note). The tool can additionally sign APKs using
 * APK Signature Scheme v2.
 */
class SignApk {
    private static final String CERT_SF_NAME = "META-INF/CERT.SF";
    private static final String CERT_SIG_NAME = "META-INF/CERT.%s";
    private static final String CERT_SF_MULTI_NAME = "META-INF/CERT%d.SF";
    private static final String CERT_SIG_MULTI_NAME = "META-INF/CERT%d.%s";

    private static final String OTACERT_NAME = "META-INF/com/android/otacert";

    // bitmasks for which hash algorithms we need the manifest to include.
    private static final int USE_SHA1 = 1;
    private static final int USE_SHA256 = 2;

    /** Digest algorithm used when signing the APK using APK Signature Scheme v2. */
    private static final String APK_SIG_SCHEME_V2_DIGEST_ALGORITHM = "SHA-256";

    /**
     * Minimum Android SDK API Level which accepts JAR signatures which use SHA-256. Older platform
     * versions accept only SHA-1 signatures.
     */
    private static final int MIN_API_LEVEL_FOR_SHA256_JAR_SIGNATURES = 18;

    /**
     * Return one of USE_SHA1 or USE_SHA256 according to the signature
     * algorithm specified in the cert.
     */
    private static int getDigestAlgorithm(X509Certificate cert, int minSdkVersion) {
        String sigAlg = cert.getSigAlgName().toUpperCase(Locale.US);
        if ("SHA1WITHRSA".equals(sigAlg) || "MD5WITHRSA".equals(sigAlg)) {
            // see "HISTORICAL NOTE" above.
            if (minSdkVersion < MIN_API_LEVEL_FOR_SHA256_JAR_SIGNATURES) {
                return USE_SHA1;
            } else {
                return USE_SHA256;
            }
        } else if (sigAlg.startsWith("SHA256WITH")) {
            return USE_SHA256;
        } else {
            throw new IllegalArgumentException("unsupported signature algorithm \"" + sigAlg +
                                               "\" in cert [" + cert.getSubjectDN());
        }
    }

    /** Returns the expected signature algorithm for this key type. */
    private static String getSignatureAlgorithm(X509Certificate cert, int minSdkVersion) {
        String keyType = cert.getPublicKey().getAlgorithm().toUpperCase(Locale.US);
        if ("RSA".equalsIgnoreCase(keyType)) {
            if ((minSdkVersion >= MIN_API_LEVEL_FOR_SHA256_JAR_SIGNATURES)
                    || (getDigestAlgorithm(cert, minSdkVersion) == USE_SHA256)) {
                return "SHA256withRSA";
            } else {
                return "SHA1withRSA";
            }
        } else if ("EC".equalsIgnoreCase(keyType)) {
            return "SHA256withECDSA";
        } else {
            throw new IllegalArgumentException("unsupported key type: " + keyType);
        }
    }

    // Files matching this pattern are not copied to the output.
    private static Pattern stripPattern =
        Pattern.compile("^(META-INF/((.*)[.](SF|RSA|DSA|EC)|com/android/otacert))|(" +
                        Pattern.quote(JarFile.MANIFEST_NAME) + ")$");

    private static X509Certificate readPublicKey(File file)
        throws IOException, GeneralSecurityException {
        FileInputStream input = new FileInputStream(file);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(input);
        } finally {
            input.close();
        }
    }

    /**
     * If a console doesn't exist, reads the password from stdin
     * If a console exists, reads the password from console and returns it as a string.
     *
     * @param keyFile The file containing the private key.  Used to prompt the user.
     */
    private static String readPassword(File keyFile) {
        Console console;
        char[] pwd;
        if ((console = System.console()) == null) {
            System.out.print("Enter password for " + keyFile + " (password will not be hidden): ");
            System.out.flush();
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            try {
                return stdin.readLine();
            } catch (IOException ex) {
                return null;
            }
        } else {
            if ((pwd = console.readPassword("[%s]", "Enter password for " + keyFile)) != null) {
                return String.valueOf(pwd);
            } else {
                return null;
            }
        }
    }

    /**
     * Decrypt an encrypted PKCS#8 format private key.
     *
     * Based on ghstark's post on Aug 6, 2006 at
     * http://forums.sun.com/thread.jspa?threadID=758133&messageID=4330949
     *
     * @param encryptedPrivateKey The raw data of the private key
     * @param keyFile The file containing the private key
     */
    private static PKCS8EncodedKeySpec decryptPrivateKey(byte[] encryptedPrivateKey, File keyFile)
        throws GeneralSecurityException {
        EncryptedPrivateKeyInfo epkInfo;
        try {
            epkInfo = new EncryptedPrivateKeyInfo(encryptedPrivateKey);
        } catch (IOException ex) {
            // Probably not an encrypted key.
            return null;
        }

        char[] password = readPassword(keyFile).toCharArray();

        SecretKeyFactory skFactory = SecretKeyFactory.getInstance(epkInfo.getAlgName());
        Key key = skFactory.generateSecret(new PBEKeySpec(password));

        Cipher cipher = Cipher.getInstance(epkInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, key, epkInfo.getAlgParameters());

        try {
            return epkInfo.getKeySpec(cipher);
        } catch (InvalidKeySpecException ex) {
            System.err.println("signapk: Password for " + keyFile + " may be bad.");
            throw ex;
        }
    }

    /** Read a PKCS#8 format private key. */
    private static PrivateKey readPrivateKey(File file)
        throws IOException, GeneralSecurityException {
        DataInputStream input = new DataInputStream(new FileInputStream(file));
        try {
            byte[] bytes = new byte[(int) file.length()];
            input.read(bytes);

            /* Check to see if this is in an EncryptedPrivateKeyInfo structure. */
            PKCS8EncodedKeySpec spec = decryptPrivateKey(bytes, file);
            if (spec == null) {
                spec = new PKCS8EncodedKeySpec(bytes);
            }

            /*
             * Now it's in a PKCS#8 PrivateKeyInfo structure. Read its Algorithm
             * OID and use that to construct a KeyFactory.
             */
            PrivateKeyInfo pki;
            try (ASN1InputStream bIn =
                    new ASN1InputStream(new ByteArrayInputStream(spec.getEncoded()))) {
                pki = PrivateKeyInfo.getInstance(bIn.readObject());
            }
            String algOid = pki.getPrivateKeyAlgorithm().getAlgorithm().getId();

            return KeyFactory.getInstance(algOid).generatePrivate(spec);
        } finally {
            input.close();
        }
    }

    /**
     * Add the hash(es) of every file to the manifest, creating it if
     * necessary.
     */
    private static Manifest addDigestsToManifest(JarFile jar, int hashes)
        throws IOException, GeneralSecurityException {
        Manifest input = jar.getManifest();
        Manifest output = new Manifest();
        Attributes main = output.getMainAttributes();
        if (input != null) {
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk)");
        }

        MessageDigest md_sha1 = null;
        MessageDigest md_sha256 = null;
        if ((hashes & USE_SHA1) != 0) {
            md_sha1 = MessageDigest.getInstance("SHA1");
        }
        if ((hashes & USE_SHA256) != 0) {
            md_sha256 = MessageDigest.getInstance("SHA256");
        }

        byte[] buffer = new byte[4096];
        int num;

        // We sort the input entries by name, and add them to the
        // output manifest in sorted order.  We expect that the output
        // map will be deterministic.

        TreeMap<String, JarEntry> byName = new TreeMap<String, JarEntry>();

        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
        }

        for (JarEntry entry: byName.values()) {
            String name = entry.getName();
            if (!entry.isDirectory() &&
                (stripPattern == null || !stripPattern.matcher(name).matches())) {
                InputStream data = jar.getInputStream(entry);
                while ((num = data.read(buffer)) > 0) {
                    if (md_sha1 != null) md_sha1.update(buffer, 0, num);
                    if (md_sha256 != null) md_sha256.update(buffer, 0, num);
                }

                Attributes attr = null;
                if (input != null) attr = input.getAttributes(name);
                attr = attr != null ? new Attributes(attr) : new Attributes();
                // Remove any previously computed digests from this entry's attributes.
                for (Iterator<Object> i = attr.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    if (!(key instanceof Attributes.Name)) {
                        continue;
                    }
                    String attributeNameLowerCase =
                            ((Attributes.Name) key).toString().toLowerCase(Locale.US);
                    if (attributeNameLowerCase.endsWith("-digest")) {
                        i.remove();
                    }
                }
                // Add SHA-1 digest if requested
                if (md_sha1 != null) {
                    attr.putValue("SHA1-Digest",
                                  new String(Base64.encode(md_sha1.digest()), "ASCII"));
                }
                // Add SHA-256 digest if requested
                if (md_sha256 != null) {
                    attr.putValue("SHA-256-Digest",
                                  new String(Base64.encode(md_sha256.digest()), "ASCII"));
                }
                output.getEntries().put(name, attr);
            }
        }

        return output;
    }

    /**
     * Add a copy of the public key to the archive; this should
     * exactly match one of the files in
     * /system/etc/security/otacerts.zip on the device.  (The same
     * cert can be extracted from the CERT.RSA file but this is much
     * easier to get at.)
     */
    private static void addOtacert(JarOutputStream outputJar,
                                   File publicKeyFile,
                                   long timestamp,
                                   Manifest manifest,
                                   int hash)
        throws IOException, GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance(hash == USE_SHA1 ? "SHA1" : "SHA256");

        JarEntry je = new JarEntry(OTACERT_NAME);
        je.setTime(timestamp);
        outputJar.putNextEntry(je);
        FileInputStream input = new FileInputStream(publicKeyFile);
        byte[] b = new byte[4096];
        int read;
        while ((read = input.read(b)) != -1) {
            outputJar.write(b, 0, read);
            md.update(b, 0, read);
        }
        input.close();

        Attributes attr = new Attributes();
        attr.putValue(hash == USE_SHA1 ? "SHA1-Digest" : "SHA-256-Digest",
                      new String(Base64.encode(md.digest()), "ASCII"));
        manifest.getEntries().put(OTACERT_NAME, attr);
    }


    /** Write to another stream and track how many bytes have been
     *  written.
     */
    private static class CountOutputStream extends FilterOutputStream {
        private int mCount;

        public CountOutputStream(OutputStream out) {
            super(out);
            mCount = 0;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            mCount++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            mCount += len;
        }

        public int size() {
            return mCount;
        }
    }

    /** Write a .SF file with a digest of the specified manifest. */
    private static void writeSignatureFile(Manifest manifest, OutputStream out,
            int hash, boolean additionallySignedUsingAnApkSignatureScheme)
        throws IOException, GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android SignApk)");
        if (additionallySignedUsingAnApkSignatureScheme) {
            // Add APK Signature Scheme v2 signature stripping protection.
            // This attribute indicates that this APK is supposed to have been signed using one or
            // more APK-specific signature schemes in addition to the standard JAR signature scheme
            // used by this code. APK signature verifier should reject the APK if it does not
            // contain a signature for the signature scheme the verifier prefers out of this set.
            main.putValue(
                    ApkSignerV2.SF_ATTRIBUTE_ANDROID_APK_SIGNED_NAME,
                    ApkSignerV2.SF_ATTRIBUTE_ANDROID_APK_SIGNED_VALUE);
        }

        MessageDigest md = MessageDigest.getInstance(
            hash == USE_SHA256 ? "SHA256" : "SHA1");
        PrintStream print = new PrintStream(
            new DigestOutputStream(new ByteArrayOutputStream(), md),
            true, "UTF-8");

        // Digest of the entire manifest 针对整个MANIFEST.MF文件，生成数据摘要，然后再生成Base64编码
        manifest.write(print);
        print.flush();
        main.putValue(hash == USE_SHA256 ? "SHA-256-Digest-Manifest" : "SHA1-Digest-Manifest",
                      new String(Base64.encode(md.digest()), "ASCII"));

        Map<String, Attributes> entries = manifest.getEntries();//此manifest为MANIFEST.MF文件对应的Manifest
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            print.print("Name: " + entry.getKey() + "\r\n");
            for (Map.Entry<Object, Object> att : entry.getValue().entrySet()) {
                print.print(att.getKey() + ": " + att.getValue() + "\r\n");
            }
            print.print("\r\n");
            print.flush();
            //针对MANIFEST.MF文件中的某一块，再生成其数据摘要的Base64编码
            Attributes sfAttr = new Attributes();
            sfAttr.putValue(hash == USE_SHA256 ? "SHA-256-Digest" : "SHA1-Digest",
                            new String(Base64.encode(md.digest()), "ASCII"));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }

        CountOutputStream cout = new CountOutputStream(out);
        sf.write(cout);

        // A bug in the java.util.jar implementation of Android platforms
        // up to version 1.6 will cause a spurious IOException to be thrown
        // if the length of the signature file is a multiple of 1024 bytes.
        // As a workaround, add an extra CRLF in this case.
        if ((cout.size() % 1024) == 0) {
            cout.write('\r');
            cout.write('\n');
        }
    }

    /** Sign data and write the digital signature to 'out'. */
    private static void writeSignatureBlock(
        CMSTypedData data, X509Certificate publicKey, PrivateKey privateKey, int minSdkVersion,
        OutputStream out)
        throws IOException,
               CertificateEncodingException,
               OperatorCreationException,
               CMSException {
        ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>(1);
        certList.add(publicKey);
        JcaCertStore certs = new JcaCertStore(certList);

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer =
                new JcaContentSignerBuilder(getSignatureAlgorithm(publicKey, minSdkVersion))
                        .build(privateKey);
        gen.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                .build())
            .setDirectSignature(true)
            .build(signer, publicKey));
        gen.addCertificates(certs);
        CMSSignedData sigData = gen.generate(data, false);

        try (ASN1InputStream asn1 = new ASN1InputStream(sigData.getEncoded())) {
            DEROutputStream dos = new DEROutputStream(out);
            dos.writeObject(asn1.readObject());
        }
    }

    /**
     * Copy all the files in a manifest from input to output.  We set
     * the modification times in the output to a fixed time, so as to
     * reduce variation in the output file and make incremental OTAs
     * more efficient.
     */
    private static void copyFiles(Manifest manifest, JarFile in, JarOutputStream out,
                                  long timestamp, int defaultAlignment) throws IOException {
        byte[] buffer = new byte[4096];
        int num;

        Map<String, Attributes> entries = manifest.getEntries();
        ArrayList<String> names = new ArrayList<String>(entries.keySet());
        Collections.sort(names);

        boolean firstEntry = true;
        long offset = 0L;

        // We do the copy in two passes -- first copying all the
        // entries that are STORED, then copying all the entries that
        // have any other compression flag (which in practice means
        // DEFLATED).  This groups all the stored entries together at
        // the start of the file and makes it easier to do alignment
        // on them (since only stored entries are aligned).

        for (String name : names) {
            JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            if (inEntry.getMethod() != JarEntry.STORED) continue;
            // Preserve the STORED method of the input entry.
            outEntry = new JarEntry(inEntry);
            outEntry.setTime(timestamp);
            // Discard comment and extra fields of this entry to
            // simplify alignment logic below and for consistency with
            // how compressed entries are handled later.
            outEntry.setComment(null);
            outEntry.setExtra(null);

            // 'offset' is the offset into the file at which we expect
            // the file data to begin.  This is the value we need to
            // make a multiple of 'alignement'.
            offset += JarFile.LOCHDR + outEntry.getName().length();
            if (firstEntry) {
                // The first entry in a jar file has an extra field of
                // four bytes that you can't get rid of; any extra
                // data you specify in the JarEntry is appended to
                // these forced four bytes.  This is JAR_MAGIC in
                // JarOutputStream; the bytes are 0xfeca0000.
                offset += 4;
                firstEntry = false;
            }
            int alignment = getStoredEntryDataAlignment(name, defaultAlignment);
            if (alignment > 0 && (offset % alignment != 0)) {
                // Set the "extra data" of the entry to between 1 and
                // alignment-1 bytes, to make the file data begin at
                // an aligned offset.
                int needed = alignment - (int)(offset % alignment);
                outEntry.setExtra(new byte[needed]);
                offset += needed;
            }

            out.putNextEntry(outEntry);

            InputStream data = in.getInputStream(inEntry);
            while ((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
                offset += num;
            }
            out.flush();
        }

        // Copy all the non-STORED entries.  We don't attempt to
        // maintain the 'offset' variable past this point; we don't do
        // alignment on these entries.

        for (String name : names) {
            JarEntry inEntry = in.getJarEntry(name);
            JarEntry outEntry = null;
            if (inEntry.getMethod() == JarEntry.STORED) continue;
            // Create a new entry so that the compressed len is recomputed.
            outEntry = new JarEntry(name);
            outEntry.setTime(timestamp);
            out.putNextEntry(outEntry);

            InputStream data = in.getInputStream(inEntry);
            while ((num = data.read(buffer)) > 0) {
                out.write(buffer, 0, num);
            }
            out.flush();
        }
    }

    /**
     * Returns the multiple (in bytes) at which the provided {@code STORED} entry's data must start
     * relative to start of file or {@code 0} if alignment of this entry's data is not important.
     */
    private static int getStoredEntryDataAlignment(String entryName, int defaultAlignment) {
        if (defaultAlignment <= 0) {
            return 0;
        }

        if (entryName.endsWith(".so")) {
            // Align .so contents to memory page boundary to enable memory-mapped
            // execution.
            return 4096;
        } else {
            return defaultAlignment;
        }
    }

    private static class WholeFileSignerOutputStream extends FilterOutputStream {
        private boolean closing = false;
        private ByteArrayOutputStream footer = new ByteArrayOutputStream();
        private OutputStream tee;

        public WholeFileSignerOutputStream(OutputStream out, OutputStream tee) {
            super(out);
            this.tee = tee;
        }

        public void notifyClosing() {
            closing = true;
        }

        public void finish() throws IOException {
            closing = false;

            byte[] data = footer.toByteArray();
            if (data.length < 2)
                throw new IOException("Less than two bytes written to footer");
            write(data, 0, data.length - 2);
        }

        public byte[] getTail() {
            return footer.toByteArray();
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closing) {
                // if the jar is about to close, save the footer that will be written
                footer.write(b, off, len);
            }
            else {
                // write to both output streams. out is the CMSTypedData signer and tee is the file.
                out.write(b, off, len);
                tee.write(b, off, len);
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (closing) {
                // if the jar is about to close, save the footer that will be written
                footer.write(b);
            }
            else {
                // write to both output streams. out is the CMSTypedData signer and tee is the file.
                out.write(b);
                tee.write(b);
            }
        }
    }

    private static class CMSSigner implements CMSTypedData {
        private final JarFile inputJar;
        private final File publicKeyFile;
        private final X509Certificate publicKey;
        private final PrivateKey privateKey;
        private final long timestamp;
        private final int minSdkVersion;
        private final OutputStream outputStream;
        private final ASN1ObjectIdentifier type;
        private WholeFileSignerOutputStream signer;

        public CMSSigner(JarFile inputJar, File publicKeyFile,
                         X509Certificate publicKey, PrivateKey privateKey, long timestamp,
                         int minSdkVersion, OutputStream outputStream) {
            this.inputJar = inputJar;
            this.publicKeyFile = publicKeyFile;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.timestamp = timestamp;
            this.minSdkVersion = minSdkVersion;
            this.outputStream = outputStream;
            this.type = new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId());
        }

        /**
         * This should actually return byte[] or something similar, but nothing
         * actually checks it currently.
         */
        @Override
        public Object getContent() {
            return this;
        }

        @Override
        public ASN1ObjectIdentifier getContentType() {
            return type;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            try {
                signer = new WholeFileSignerOutputStream(out, outputStream);
                JarOutputStream outputJar = new JarOutputStream(signer);

                int hash = getDigestAlgorithm(publicKey, minSdkVersion);

                Manifest manifest = addDigestsToManifest(inputJar, hash);
                copyFiles(manifest, inputJar, outputJar, timestamp, 0);
                addOtacert(outputJar, publicKeyFile, timestamp, manifest, hash);

                signFile(manifest,
                         new X509Certificate[]{ publicKey },
                         new PrivateKey[]{ privateKey },
                         timestamp,
                         minSdkVersion,
                         false, // Don't sign using APK Signature Scheme v2
                         outputJar);

                signer.notifyClosing();
                outputJar.close();
                signer.finish();
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }

        public void writeSignatureBlock(ByteArrayOutputStream temp)
            throws IOException,
                   CertificateEncodingException,
                   OperatorCreationException,
                   CMSException {
            SignApk.writeSignatureBlock(this, publicKey, privateKey, minSdkVersion, temp);
        }

        public WholeFileSignerOutputStream getSigner() {
            return signer;
        }
    }

    private static void signWholeFile(JarFile inputJar, File publicKeyFile,
                                      X509Certificate publicKey, PrivateKey privateKey,
                                      long timestamp, int minSdkVersion,
                                      OutputStream outputStream) throws Exception {
        CMSSigner cmsOut = new CMSSigner(inputJar, publicKeyFile,
                publicKey, privateKey, timestamp, minSdkVersion, outputStream);

        ByteArrayOutputStream temp = new ByteArrayOutputStream();

        // put a readable message and a null char at the start of the
        // archive comment, so that tools that display the comment
        // (hopefully) show something sensible.
        // TODO: anything more useful we can put in this message?
        byte[] message = "signed by SignApk".getBytes("UTF-8");
        temp.write(message);
        temp.write(0);

        cmsOut.writeSignatureBlock(temp);

        byte[] zipData = cmsOut.getSigner().getTail();

        // For a zip with no archive comment, the
        // end-of-central-directory record will be 22 bytes long, so
        // we expect to find the EOCD marker 22 bytes from the end.
        if (zipData[zipData.length-22] != 0x50 ||
            zipData[zipData.length-21] != 0x4b ||
            zipData[zipData.length-20] != 0x05 ||
            zipData[zipData.length-19] != 0x06) {
            throw new IllegalArgumentException("zip data already has an archive comment");
        }

        int total_size = temp.size() + 6;
        if (total_size > 0xffff) {
            throw new IllegalArgumentException("signature is too big for ZIP file comment");
        }
        // signature starts this many bytes from the end of the file
        int signature_start = total_size - message.length - 1;
        temp.write(signature_start & 0xff);
        temp.write((signature_start >> 8) & 0xff);
        // Why the 0xff bytes?  In a zip file with no archive comment,
        // bytes [-6:-2] of the file are the little-endian offset from
        // the start of the file to the central directory.  So for the
        // two high bytes to be 0xff 0xff, the archive would have to
        // be nearly 4GB in size.  So it's unlikely that a real
        // commentless archive would have 0xffs here, and lets us tell
        // an old signed archive from a new one.
        temp.write(0xff);
        temp.write(0xff);
        temp.write(total_size & 0xff);
        temp.write((total_size >> 8) & 0xff);
        temp.flush();

        // Signature verification checks that the EOCD header is the
        // last such sequence in the file (to avoid minzip finding a
        // fake EOCD appended after the signature in its scan).  The
        // odds of producing this sequence by chance are very low, but
        // let's catch it here if it does.
        byte[] b = temp.toByteArray();
        for (int i = 0; i < b.length-3; ++i) {
            if (b[i] == 0x50 && b[i+1] == 0x4b && b[i+2] == 0x05 && b[i+3] == 0x06) {
                throw new IllegalArgumentException("found spurious EOCD header at " + i);
            }
        }

        outputStream.write(total_size & 0xff);
        outputStream.write((total_size >> 8) & 0xff);
        temp.writeTo(outputStream);
    }

    private static void signFile(Manifest manifest,
                                 X509Certificate[] publicKey, PrivateKey[] privateKey,
                                 long timestamp,
                                 int minSdkVersion,
                                 boolean additionallySignedUsingAnApkSignatureScheme,
                                 JarOutputStream outputJar)
        throws Exception {

        // MANIFEST.MF 生成META-INF/MANIFEST.MF文件（Manifest就表示了MANIFEST.MF文件）
        JarEntry je = new JarEntry(JarFile.MANIFEST_NAME);
        je.setTime(timestamp);
        outputJar.putNextEntry(je);
        manifest.write(outputJar);//1.生成MANIFEST.MF文件

        int numKeys = publicKey.length;
        for (int k = 0; k < numKeys; ++k) {
            // CERT.SF / CERT#.SF
            je = new JarEntry(numKeys == 1 ? CERT_SF_NAME :
                              (String.format(CERT_SF_MULTI_NAME, k)));
            je.setTime(timestamp);
            outputJar.putNextEntry(je);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();//即META-INF/CERT.SF文件内容
            writeSignatureFile(
                    manifest,
                    baos,
                    getDigestAlgorithm(publicKey[k], minSdkVersion),
                    additionallySignedUsingAnApkSignatureScheme);
            byte[] signedData = baos.toByteArray();
            outputJar.write(signedData);//2.生成META-INF/CERT.SF文件

            // CERT.{EC,RSA} / CERT#.{EC,RSA}
            final String keyType = publicKey[k].getPublicKey().getAlgorithm();
            je = new JarEntry(numKeys == 1 ?
                              (String.format(CERT_SIG_NAME, keyType)) :
                              (String.format(CERT_SIG_MULTI_NAME, k, keyType)));
            je.setTime(timestamp);
            outputJar.putNextEntry(je);//META-INF/CERT.%s文件
            writeSignatureBlock(new CMSProcessableByteArray(signedData),
                                publicKey[k], privateKey[k], minSdkVersion, outputJar);//3.生成CERT.RSA文件
        }
    }

    /**
     * Tries to load a JSE Provider by class name. This is for custom PrivateKey
     * types that might be stored in PKCS#11-like storage.
     */
    private static void loadProviderIfNecessary(String providerClassName) {
        if (providerClassName == null) {
            return;
        }

        final Class<?> klass;
        try {
            final ClassLoader sysLoader = ClassLoader.getSystemClassLoader();
            if (sysLoader != null) {
                klass = sysLoader.loadClass(providerClassName);
            } else {
                klass = Class.forName(providerClassName);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        Constructor<?> constructor = null;
        for (Constructor<?> c : klass.getConstructors()) {
            if (c.getParameterTypes().length == 0) {
                constructor = c;
                break;
            }
        }
        if (constructor == null) {
            System.err.println("No zero-arg constructor found for " + providerClassName);
            System.exit(1);
            return;
        }

        final Object o;
        try {
            o = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        if (!(o instanceof Provider)) {
            System.err.println("Not a Provider class: " + providerClassName);
            System.exit(1);
        }

        Security.insertProviderAt((Provider) o, 1);
    }

    /**
     * Converts the provided lists of private keys, their X.509 certificates, and digest algorithms
     * into a list of APK Signature Scheme v2 {@code SignerConfig} instances.
     */
    public static List<ApkSignerV2.SignerConfig> createV2SignerConfigs(
            PrivateKey[] privateKeys, X509Certificate[] certificates, String[] digestAlgorithms)
                    throws InvalidKeyException {
        if (privateKeys.length != certificates.length) {
            throw new IllegalArgumentException(
                    "The number of private keys must match the number of certificates: "
                            + privateKeys.length + " vs" + certificates.length);
        }
        List<ApkSignerV2.SignerConfig> result = new ArrayList<>(privateKeys.length);
        for (int i = 0; i < privateKeys.length; i++) {
            PrivateKey privateKey = privateKeys[i];
            X509Certificate certificate = certificates[i];
            PublicKey publicKey = certificate.getPublicKey();
            String keyAlgorithm = privateKey.getAlgorithm();
            if (!keyAlgorithm.equalsIgnoreCase(publicKey.getAlgorithm())) {
                throw new InvalidKeyException(
                        "Key algorithm of private key #" + (i + 1) + " does not match key"
                        + " algorithm of public key #" + (i + 1) + ": " + keyAlgorithm
                        + " vs " + publicKey.getAlgorithm());
            }
            ApkSignerV2.SignerConfig signerConfig = new ApkSignerV2.SignerConfig();
            signerConfig.privateKey = privateKey;
            signerConfig.certificates = Collections.singletonList(certificate);
            List<Integer> signatureAlgorithms = new ArrayList<>(digestAlgorithms.length);
            for (String digestAlgorithm : digestAlgorithms) {
                try {
                    signatureAlgorithms.add(
                            getV2SignatureAlgorithm(keyAlgorithm, digestAlgorithm));
                } catch (IllegalArgumentException e) {
                    throw new InvalidKeyException(
                            "Unsupported key and digest algorithm combination for signer #"
                                    + (i + 1),
                            e);
                }
            }
            signerConfig.signatureAlgorithms = signatureAlgorithms;
            result.add(signerConfig);
        }
        return result;
    }

    private static int getV2SignatureAlgorithm(String keyAlgorithm, String digestAlgorithm) {
        if ("SHA-256".equalsIgnoreCase(digestAlgorithm)) {
            if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
                // Use RSASSA-PKCS1-v1_5 signature scheme instead of RSASSA-PSS to guarantee
                // deterministic signatures which make life easier for OTA updates (fewer files
                // changed when deterministic signature schemes are used).
                return ApkSignerV2.SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256;
            } else if ("EC".equalsIgnoreCase(keyAlgorithm)) {
                return ApkSignerV2.SIGNATURE_ECDSA_WITH_SHA256;
            } else if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
                return ApkSignerV2.SIGNATURE_DSA_WITH_SHA256;
            } else {
                throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
            }
        } else if ("SHA-512".equalsIgnoreCase(digestAlgorithm)) {
            if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
                // Use RSASSA-PKCS1-v1_5 signature scheme instead of RSASSA-PSS to guarantee
                // deterministic signatures which make life easier for OTA updates (fewer files
                // changed when deterministic signature schemes are used).
                return ApkSignerV2.SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA512;
            } else if ("EC".equalsIgnoreCase(keyAlgorithm)) {
                return ApkSignerV2.SIGNATURE_ECDSA_WITH_SHA512;
            } else if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
                return ApkSignerV2.SIGNATURE_DSA_WITH_SHA512;
            } else {
                throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
            }
        } else {
            throw new IllegalArgumentException("Unsupported digest algorithm: " + digestAlgorithm);
        }
    }

    private static void usage() {
        System.err.println("Usage: signapk [-w] " +
                           "[-a <alignment>] " +
                           "[-providerClass <className>] " +
                           "[--min-sdk-version <n>] " +
                           "[--disable-v2] " +
                           "publickey.x509[.pem] privatekey.pk8 " +
                           "[publickey2.x509[.pem] privatekey2.pk8 ...] " +
                           "input.jar output.jar");
        System.exit(2);
    }

    public static void main(String[] args) {
        if (args.length < 4) usage();

        // Install Conscrypt as the highest-priority provider. Its crypto primitives are faster than
        // the standard or Bouncy Castle ones.
        Security.insertProviderAt(new OpenSSLProvider(), 1);
        // Install Bouncy Castle (as the lowest-priority provider) because Conscrypt does not offer
        // DSA which may still be needed.
        // TODO: Stop installing Bouncy Castle provider once DSA is no longer needed.
        Security.addProvider(new BouncyCastleProvider());

        boolean signWholeFile = false;
        String providerClass = null;
        int alignment = 4;
        int minSdkVersion = 0;
        boolean signUsingApkSignatureSchemeV2 = true;

        int argstart = 0;
        while (argstart < args.length && args[argstart].startsWith("-")) {
            if ("-w".equals(args[argstart])) {
                signWholeFile = true;
                ++argstart;
            } else if ("-providerClass".equals(args[argstart])) {
                if (argstart + 1 >= args.length) {
                    usage();
                }
                providerClass = args[++argstart];
                ++argstart;
            } else if ("-a".equals(args[argstart])) {
                alignment = Integer.parseInt(args[++argstart]);
                ++argstart;
            } else if ("--min-sdk-version".equals(args[argstart])) {
                String minSdkVersionString = args[++argstart];
                try {
                    minSdkVersion = Integer.parseInt(minSdkVersionString);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "--min-sdk-version must be a decimal number: " + minSdkVersionString);
                }
                ++argstart;
            } else if ("--disable-v2".equals(args[argstart])) {
                signUsingApkSignatureSchemeV2 = false;
                ++argstart;
            } else {
                usage();
            }
        }

        if ((args.length - argstart) % 2 == 1) usage();
        int numKeys = ((args.length - argstart) / 2) - 1;//签名对个数
        if (signWholeFile && numKeys > 1) {
            System.err.println("Only one key may be used with -w.");
            System.exit(2);
        }

        loadProviderIfNecessary(providerClass);

        String inputFilename = args[args.length-2];
        String outputFilename = args[args.length-1];

        JarFile inputJar = null;
        FileOutputStream outputFile = null;
        int hashes = 0;

        try {
            File firstPublicKeyFile = new File(args[argstart+0]);
            //公钥列表
            X509Certificate[] publicKey = new X509Certificate[numKeys];
            try {
                for (int i = 0; i < numKeys; ++i) {
                    int argNum = argstart + i*2;
                    publicKey[i] = readPublicKey(new File(args[argNum]));
                    hashes |= getDigestAlgorithm(publicKey[i], minSdkVersion);
                }
            } catch (IllegalArgumentException e) {
                System.err.println(e);
                System.exit(1);
            }

            // Set all ZIP file timestamps to Jan 1 2009 00:00:00.
            long timestamp = 1230768000000L;
            // The Java ZipEntry API we're using converts milliseconds since epoch into MS-DOS
            // timestamp using the current timezone. We thus adjust the milliseconds since epoch
            // value to end up with MS-DOS timestamp of Jan 1 2009 00:00:00.
            timestamp -= TimeZone.getDefault().getOffset(timestamp);
            //私钥列表
            PrivateKey[] privateKey = new PrivateKey[numKeys];
            for (int i = 0; i < numKeys; ++i) {
                int argNum = argstart + i*2 + 1;
                privateKey[i] = readPrivateKey(new File(args[argNum]));
            }
            inputJar = new JarFile(new File(inputFilename), false);  // Don't verify.

            outputFile = new FileOutputStream(outputFilename);

            // NOTE: Signing currently recompresses any compressed entries using Deflate (default
            // compression level for OTA update files and maximum compession level for APKs).
            if (signWholeFile) {
                SignApk.signWholeFile(inputJar, firstPublicKeyFile,
                                      publicKey[0], privateKey[0],
                                      timestamp, minSdkVersion,
                                      outputFile);
            } else {
                // Generate, in memory, an APK signed using standard JAR Signature Scheme.
                ByteArrayOutputStream v1SignedApkBuf = new ByteArrayOutputStream();
                JarOutputStream outputJar = new JarOutputStream(v1SignedApkBuf);
                // Use maximum compression for compressed entries because the APK lives forever on
                // the system partition.
                outputJar.setLevel(9);
                Manifest manifest = addDigestsToManifest(inputJar, hashes);
                copyFiles(manifest, inputJar, outputJar, timestamp, alignment);
                signFile(
                        manifest,
                        publicKey, privateKey,
                        timestamp, minSdkVersion, signUsingApkSignatureSchemeV2,
                        outputJar);//进行V1签名
                outputJar.close();
                ByteBuffer v1SignedApk = ByteBuffer.wrap(v1SignedApkBuf.toByteArray());//V1签名后的apk
                v1SignedApkBuf.reset();

                ByteBuffer[] outputChunks;
                if (signUsingApkSignatureSchemeV2) { //是否需要V2签名
                    // Additionally sign the APK using the APK Signature Scheme v2.
                    ByteBuffer apkContents = v1SignedApk;
                    List<ApkSignerV2.SignerConfig> signerConfigs =
                            createV2SignerConfigs(
                                    privateKey,
                           g         publicKey,
                                    new String[] {APK_SIG_SCHEME_V2_DIGEST_ALGORITHM});
                    outputChunks = ApkSignerV2.sign(apkContents, signerConfigs);//进行v2签名
                } else {
                    // Output the JAR-signed APK as is.
                    outputChunks = new ByteBuffer[] {v1SignedApk};
                }

                // This assumes outputChunks are array-backed. To avoid this assumption, the
                // code could be rewritten to use FileChannel.
                for (ByteBuffer outputChunk : outputChunks) { //生成最终的签名后的APK文件
                    outputFile.write(
                            outputChunk.array(),
                            outputChunk.arrayOffset() + outputChunk.position(),
                            outputChunk.remaining());
                    outputChunk.position(outputChunk.limit());
                }

                outputFile.close();
                outputFile = null;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (inputJar != null) inputJar.close();
                if (outputFile != null) outputFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
