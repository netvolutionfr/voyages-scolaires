package fr.siovision.voyages.application.utils;

import java.util.Base64;

public final class Base64Url {
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    public static byte[] decode(String s){ return DEC.decode(s); }
    public static String encode(byte[] b){ return ENC.encodeToString(b); }
}