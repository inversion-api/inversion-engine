/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.utils;


import java.util.HashMap;

public class MimeTypes {

    public static final String TYPE_APPLICATION_JSON                 = "application/json";
    public static final String TYPE_APPLICATION_ZIP                  = "application/zip";
    public static final String TYPE_APPLICATION_X_GZIP               = "application/x-gzip";
    public static final String TYPE_APPLICATION_TGZ                  = "application/tgz";
    public static final String TYPE_APPLICATION_MSWORD               = "application/msword";
    public static final String TYPE_APPLICATION_POSTSCRIPT           = "application/postscript";
    public static final String TYPE_APPLICATION_PDF                  = "application/pdf";
    public static final String TYPE_APPLICATION_JNLP                 = "application/jnlp";
    public static final String TYPE_APPLICATION_MAC_BINHEX40         = "application/mac-binhex40";
    public static final String TYPE_APPLICATION_MAC_COMPACTPRO       = "application/mac-compactpro";
    public static final String TYPE_APPLICATION_MATHML_XML           = "application/mathml+xml";
    public static final String TYPE_APPLICATION_OCTET_STREAM         = "application/octet-stream";
    public static final String TYPE_APPLICATION_ODA                  = "application/oda";
    public static final String TYPE_APPLICATION_RDF_XML              = "application/rdf+xml";
    public static final String TYPE_APPLICATION_JAVA_ARCHIVE         = "application/java-archive";
    public static final String TYPE_APPLICATION_RDF_SMIL             = "application/smil";
    public static final String TYPE_APPLICATION_SRGS                 = "application/srgs";
    public static final String TYPE_APPLICATION_SRGS_XML             = "application/srgs+xml";
    public static final String TYPE_APPLICATION_VND_MIF              = "application/vnd.mif";
    public static final String TYPE_APPLICATION_VND_MSEXCEL          = "application/vnd.ms-excel";
    public static final String TYPE_APPLICATION_VND_MSPOWERPOINT     = "application/vnd.ms-powerpoint";
    public static final String TYPE_APPLICATION_VND_RNREALMEDIA      = "application/vnd.rn-realmedia";
    public static final String TYPE_APPLICATION_X_BCPIO              = "application/x-bcpio";
    public static final String TYPE_APPLICATION_X_CDLINK             = "application/x-cdlink";
    public static final String TYPE_APPLICATION_X_CHESS_PGN          = "application/x-chess-pgn";
    public static final String TYPE_APPLICATION_X_CPIO               = "application/x-cpio";
    public static final String TYPE_APPLICATION_X_CSH                = "application/x-csh";
    public static final String TYPE_APPLICATION_X_DIRECTOR           = "application/x-director";
    public static final String TYPE_APPLICATION_X_DVI                = "application/x-dvi";
    public static final String TYPE_APPLICATION_X_FUTURESPLASH       = "application/x-futuresplash";
    public static final String TYPE_APPLICATION_X_GTAR               = "application/x-gtar";
    public static final String TYPE_APPLICATION_X_HDF                = "application/x-hdf";
    public static final String TYPE_APPLICATION_X_JAVASCRIPT         = "application/x-javascript";
    public static final String TYPE_APPLICATION_X_KOAN               = "application/x-koan";
    public static final String TYPE_APPLICATION_X_LATEX              = "application/x-latex";
    public static final String TYPE_APPLICATION_X_NETCDF             = "application/x-netcdf";
    public static final String TYPE_APPLICATION_X_OGG                = "application/x-ogg";
    public static final String TYPE_APPLICATION_X_SH                 = "application/x-sh";
    public static final String TYPE_APPLICATION_X_SHAR               = "application/x-shar";
    public static final String TYPE_APPLICATION_X_SHOCKWAVE_FLASH    = "application/x-shockwave-flash";
    public static final String TYPE_APPLICATION_X_STUFFIT            = "application/x-stuffit";
    public static final String TYPE_APPLICATION_X_SV4CPIO            = "application/x-sv4cpio";
    public static final String TYPE_APPLICATION_X_SV4CRC             = "application/x-sv4crc";
    public static final String TYPE_APPLICATION_X_TAR                = "application/x-tar";
    public static final String TYPE_APPLICATION_X_RAR_COMPRESSED     = "application/x-rar-compressed";
    public static final String TYPE_APPLICATION_X_TCL                = "application/x-tcl";
    public static final String TYPE_APPLICATION_X_TEX                = "application/x-tex";
    public static final String TYPE_APPLICATION_X_TEXINFO            = "application/x-texinfo";
    public static final String TYPE_APPLICATION_X_TROFF              = "application/x-troff";
    public static final String TYPE_APPLICATION_X_TROFF_MAN          = "application/x-troff-man";
    public static final String TYPE_APPLICATION_X_TROFF_ME           = "application/x-troff-me";
    public static final String TYPE_APPLICATION_X_TROFF_MS           = "application/x-troff-ms";
    public static final String TYPE_APPLICATION_X_USTAR              = "application/x-ustar";
    public static final String TYPE_APPLICATION_X_WAIS_SOURCE        = "application/x-wais-source";
    public static final String TYPE_APPLICATION_VND_MOZZILLA_XUL_XML = "application/vnd.mozilla.xul+xml";
    public static final String TYPE_APPLICATION_XHTML_XML            = "application/xhtml+xml";
    public static final String TYPE_APPLICATION_XSLT_XML             = "application/xslt+xml";
    public static final String TYPE_APPLICATION_XML                  = "application/xml";
    public static final String TYPE_APPLICATION_XML_DTD              = "application/xml-dtd";
    public static final String TYPE_IMAGE_BMP                        = "image/bmp";
    public static final String TYPE_IMAGE_CGM                        = "image/cgm";
    public static final String TYPE_IMAGE_GIF                        = "image/gif";
    public static final String TYPE_IMAGE_IEF                        = "image/ief";
    public static final String TYPE_IMAGE_JPEG                       = "image/jpeg";
    public static final String TYPE_IMAGE_TIFF                       = "image/tiff";
    public static final String TYPE_IMAGE_PNG                        = "image/png";
    public static final String TYPE_IMAGE_SVG_XML                    = "image/svg+xml";
    public static final String TYPE_IMAGE_VND_DJVU                   = "image/vnd.djvu";
    public static final String TYPE_IMAGE_WAP_WBMP                   = "image/vnd.wap.wbmp";
    public static final String TYPE_IMAGE_X_CMU_RASTER               = "image/x-cmu-raster";
    public static final String TYPE_IMAGE_X_ICON                     = "image/x-icon";
    public static final String TYPE_IMAGE_X_PORTABLE_ANYMAP          = "image/x-portable-anymap";
    public static final String TYPE_IMAGE_X_PORTABLE_BITMAP          = "image/x-portable-bitmap";
    public static final String TYPE_IMAGE_X_PORTABLE_GRAYMAP         = "image/x-portable-graymap";
    public static final String TYPE_IMAGE_X_PORTABLE_PIXMAP          = "image/x-portable-pixmap";
    public static final String TYPE_IMAGE_X_RGB                      = "image/x-rgb";
    public static final String TYPE_AUDIO_BASIC                      = "audio/basic";
    public static final String TYPE_AUDIO_MIDI                       = "audio/midi";
    public static final String TYPE_AUDIO_MPEG                       = "audio/mpeg";
    public static final String TYPE_AUDIO_X_AIFF                     = "audio/x-aiff";
    public static final String TYPE_AUDIO_X_MPEGURL                  = "audio/x-mpegurl";
    public static final String TYPE_AUDIO_X_PN_REALAUDIO             = "audio/x-pn-realaudio";
    public static final String TYPE_AUDIO_X_WAV                      = "audio/x-wav";
    public static final String TYPE_CHEMICAL_X_PDB                   = "chemical/x-pdb";
    public static final String TYPE_CHEMICAL_X_XYZ                   = "chemical/x-xyz";
    public static final String TYPE_MODEL_IGES                       = "model/iges";
    public static final String TYPE_MODEL_MESH                       = "model/mesh";
    public static final String TYPE_MODEL_VRLM                       = "model/vrml";
    public static final String TYPE_TEXT_PLAIN                       = "text/plain";
    public static final String TYPE_TEXT_RICHTEXT                    = "text/richtext";
    public static final String TYPE_TEXT_RTF                         = "text/rtf";
    public static final String TYPE_TEXT_HTML                        = "text/html";
    public static final String TYPE_TEXT_CALENDAR                    = "text/calendar";
    public static final String TYPE_TEXT_CSS                         = "text/css";
    public static final String TYPE_TEXT_SGML                        = "text/sgml";
    public static final String TYPE_TEXT_TAB_SEPARATED_VALUES        = "text/tab-separated-values";
    public static final String TYPE_TEXT_VND_WAP_XML                 = "text/vnd.wap.wml";
    public static final String TYPE_TEXT_VND_WAP_WMLSCRIPT           = "text/vnd.wap.wmlscript";
    public static final String TYPE_TEXT_X_SETEXT                    = "text/x-setext";
    public static final String TYPE_TEXT_X_COMPONENT                 = "text/x-component";
    public static final String TYPE_VIDEO_QUICKTIME                  = "video/quicktime";
    public static final String TYPE_VIDEO_MPEG                       = "video/mpeg";
    public static final String TYPE_VIDEO_VND_MPEGURL                = "video/vnd.mpegurl";
    public static final String TYPE_VIDEO_X_MSVIDEO                  = "video/x-msvideo";
    public static final String TYPE_VIDEO_X_MS_WMV                   = "video/x-ms-wmv";
    public static final String TYPE_VIDEO_X_SGI_MOVIE                = "video/x-sgi-movie";
    public static final String TYPE_X_CONFERENCE_X_COOLTALK          = "x-conference/x-cooltalk";

    private static HashMap<String, String> mimeTypes = new HashMap<>();

    static {
        withMimeType("xul", TYPE_APPLICATION_VND_MOZZILLA_XUL_XML);
        withMimeType("json", TYPE_APPLICATION_JSON);
        withMimeType("ice", TYPE_X_CONFERENCE_X_COOLTALK);
        withMimeType("movie", TYPE_VIDEO_X_SGI_MOVIE);
        withMimeType("avi", TYPE_VIDEO_X_MSVIDEO);
        withMimeType("wmv", TYPE_VIDEO_X_MS_WMV);
        withMimeType("m4u", TYPE_VIDEO_VND_MPEGURL);
        withMimeType("mxu", TYPE_VIDEO_VND_MPEGURL);
        withMimeType("htc", TYPE_TEXT_X_COMPONENT);
        withMimeType("etx", TYPE_TEXT_X_SETEXT);
        withMimeType("wmls", TYPE_TEXT_VND_WAP_WMLSCRIPT);
        withMimeType("wml", TYPE_TEXT_VND_WAP_XML);
        withMimeType("tsv", TYPE_TEXT_TAB_SEPARATED_VALUES);
        withMimeType("sgm", TYPE_TEXT_SGML);
        withMimeType("sgml", TYPE_TEXT_SGML);
        withMimeType("css", TYPE_TEXT_CSS);
        withMimeType("ifb", TYPE_TEXT_CALENDAR);
        withMimeType("ics", TYPE_TEXT_CALENDAR);
        withMimeType("wrl", TYPE_MODEL_VRLM);
        withMimeType("vrlm", TYPE_MODEL_VRLM);
        withMimeType("silo", TYPE_MODEL_MESH);
        withMimeType("mesh", TYPE_MODEL_MESH);
        withMimeType("msh", TYPE_MODEL_MESH);
        withMimeType("iges", TYPE_MODEL_IGES);
        withMimeType("igs", TYPE_MODEL_IGES);
        withMimeType("rgb", TYPE_IMAGE_X_RGB);
        withMimeType("ppm", TYPE_IMAGE_X_PORTABLE_PIXMAP);
        withMimeType("pgm", TYPE_IMAGE_X_PORTABLE_GRAYMAP);
        withMimeType("pbm", TYPE_IMAGE_X_PORTABLE_BITMAP);
        withMimeType("pnm", TYPE_IMAGE_X_PORTABLE_ANYMAP);
        withMimeType("ico", TYPE_IMAGE_X_ICON);
        withMimeType("ras", TYPE_IMAGE_X_CMU_RASTER);
        withMimeType("wbmp", TYPE_IMAGE_WAP_WBMP);
        withMimeType("djv", TYPE_IMAGE_VND_DJVU);
        withMimeType("djvu", TYPE_IMAGE_VND_DJVU);
        withMimeType("svg", TYPE_IMAGE_SVG_XML);
        withMimeType("ief", TYPE_IMAGE_IEF);
        withMimeType("cgm", TYPE_IMAGE_CGM);
        withMimeType("bmp", TYPE_IMAGE_BMP);
        withMimeType("xyz", TYPE_CHEMICAL_X_XYZ);
        withMimeType("pdb", TYPE_CHEMICAL_X_PDB);
        withMimeType("ra", TYPE_AUDIO_X_PN_REALAUDIO);
        withMimeType("ram", TYPE_AUDIO_X_PN_REALAUDIO);
        withMimeType("m3u", TYPE_AUDIO_X_MPEGURL);
        withMimeType("aifc", TYPE_AUDIO_X_AIFF);
        withMimeType("aif", TYPE_AUDIO_X_AIFF);
        withMimeType("aiff", TYPE_AUDIO_X_AIFF);
        withMimeType("mp3", TYPE_AUDIO_MPEG);
        withMimeType("mp2", TYPE_AUDIO_MPEG);
        withMimeType("mp1", TYPE_AUDIO_MPEG);
        withMimeType("mpga", TYPE_AUDIO_MPEG);
        withMimeType("kar", TYPE_AUDIO_MIDI);
        withMimeType("mid", TYPE_AUDIO_MIDI);
        withMimeType("midi", TYPE_AUDIO_MIDI);
        withMimeType("dtd", TYPE_APPLICATION_XML_DTD);
        withMimeType("xsl", TYPE_APPLICATION_XML);
        withMimeType("xml", TYPE_APPLICATION_XML);
        withMimeType("xslt", TYPE_APPLICATION_XSLT_XML);
        withMimeType("xht", TYPE_APPLICATION_XHTML_XML);
        withMimeType("xhtml", TYPE_APPLICATION_XHTML_XML);
        withMimeType("src", TYPE_APPLICATION_X_WAIS_SOURCE);
        withMimeType("ustar", TYPE_APPLICATION_X_USTAR);
        withMimeType("ms", TYPE_APPLICATION_X_TROFF_MS);
        withMimeType("me", TYPE_APPLICATION_X_TROFF_ME);
        withMimeType("man", TYPE_APPLICATION_X_TROFF_MAN);
        withMimeType("roff", TYPE_APPLICATION_X_TROFF);
        withMimeType("tr", TYPE_APPLICATION_X_TROFF);
        withMimeType("t", TYPE_APPLICATION_X_TROFF);
        withMimeType("texi", TYPE_APPLICATION_X_TEXINFO);
        withMimeType("texinfo", TYPE_APPLICATION_X_TEXINFO);
        withMimeType("tex", TYPE_APPLICATION_X_TEX);
        withMimeType("tcl", TYPE_APPLICATION_X_TCL);
        withMimeType("sv4crc", TYPE_APPLICATION_X_SV4CRC);
        withMimeType("sv4cpio", TYPE_APPLICATION_X_SV4CPIO);
        withMimeType("sit", TYPE_APPLICATION_X_STUFFIT);
        withMimeType("swf", TYPE_APPLICATION_X_SHOCKWAVE_FLASH);
        withMimeType("shar", TYPE_APPLICATION_X_SHAR);
        withMimeType("sh", TYPE_APPLICATION_X_SH);
        withMimeType("cdf", TYPE_APPLICATION_X_NETCDF);
        withMimeType("nc", TYPE_APPLICATION_X_NETCDF);
        withMimeType("latex", TYPE_APPLICATION_X_LATEX);
        withMimeType("skm", TYPE_APPLICATION_X_KOAN);
        withMimeType("skt", TYPE_APPLICATION_X_KOAN);
        withMimeType("skd", TYPE_APPLICATION_X_KOAN);
        withMimeType("skp", TYPE_APPLICATION_X_KOAN);
        withMimeType("js", TYPE_APPLICATION_X_JAVASCRIPT);
        withMimeType("hdf", TYPE_APPLICATION_X_HDF);
        withMimeType("gtar", TYPE_APPLICATION_X_GTAR);
        withMimeType("spl", TYPE_APPLICATION_X_FUTURESPLASH);
        withMimeType("dvi", TYPE_APPLICATION_X_DVI);
        withMimeType("dxr", TYPE_APPLICATION_X_DIRECTOR);
        withMimeType("dir", TYPE_APPLICATION_X_DIRECTOR);
        withMimeType("dcr", TYPE_APPLICATION_X_DIRECTOR);
        withMimeType("csh", TYPE_APPLICATION_X_CSH);
        withMimeType("cpio", TYPE_APPLICATION_X_CPIO);
        withMimeType("pgn", TYPE_APPLICATION_X_CHESS_PGN);
        withMimeType("vcd", TYPE_APPLICATION_X_CDLINK);
        withMimeType("bcpio", TYPE_APPLICATION_X_BCPIO);
        withMimeType("rm", TYPE_APPLICATION_VND_RNREALMEDIA);
        withMimeType("ppt", TYPE_APPLICATION_VND_MSPOWERPOINT);
        withMimeType("mif", TYPE_APPLICATION_VND_MIF);
        withMimeType("grxml", TYPE_APPLICATION_SRGS_XML);
        withMimeType("gram", TYPE_APPLICATION_SRGS);
        withMimeType("smil", TYPE_APPLICATION_RDF_SMIL);
        withMimeType("smi", TYPE_APPLICATION_RDF_SMIL);
        withMimeType("rdf", TYPE_APPLICATION_RDF_XML);
        withMimeType("ogg", TYPE_APPLICATION_X_OGG);
        withMimeType("oda", TYPE_APPLICATION_ODA);
        withMimeType("dmg", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("lzh", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("so", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("lha", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("dms", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("bin", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("mathml", TYPE_APPLICATION_MATHML_XML);
        withMimeType("cpt", TYPE_APPLICATION_MAC_COMPACTPRO);
        withMimeType("hqx", TYPE_APPLICATION_MAC_BINHEX40);
        withMimeType("jnlp", TYPE_APPLICATION_JNLP);
        withMimeType("txt", TYPE_TEXT_PLAIN);
        withMimeType("ini", TYPE_TEXT_PLAIN);
        withMimeType("c", TYPE_TEXT_PLAIN);
        withMimeType("h", TYPE_TEXT_PLAIN);
        withMimeType("cpp", TYPE_TEXT_PLAIN);
        withMimeType("cxx", TYPE_TEXT_PLAIN);
        withMimeType("cc", TYPE_TEXT_PLAIN);
        withMimeType("chh", TYPE_TEXT_PLAIN);
        withMimeType("java", TYPE_TEXT_PLAIN);
        withMimeType("csv", TYPE_TEXT_PLAIN);
        withMimeType("bat", TYPE_TEXT_PLAIN);
        withMimeType("cmd", TYPE_TEXT_PLAIN);
        withMimeType("asc", TYPE_TEXT_PLAIN);
        withMimeType("rtf", TYPE_TEXT_RTF);
        withMimeType("rtx", TYPE_TEXT_RICHTEXT);
        withMimeType("html", TYPE_TEXT_HTML);
        withMimeType("htm", TYPE_TEXT_HTML);
        withMimeType("zip", TYPE_APPLICATION_ZIP);
        withMimeType("rar", TYPE_APPLICATION_X_RAR_COMPRESSED);
        withMimeType("gzip", TYPE_APPLICATION_X_GZIP);
        withMimeType("gz", TYPE_APPLICATION_X_GZIP);
        withMimeType("tgz", TYPE_APPLICATION_TGZ);
        withMimeType("tar", TYPE_APPLICATION_X_TAR);
        withMimeType("gif", TYPE_IMAGE_GIF);
        withMimeType("jpeg", TYPE_IMAGE_JPEG);
        withMimeType("jpg", TYPE_IMAGE_JPEG);
        withMimeType("jpe", TYPE_IMAGE_JPEG);
        withMimeType("tiff", TYPE_IMAGE_TIFF);
        withMimeType("tif", TYPE_IMAGE_TIFF);
        withMimeType("png", TYPE_IMAGE_PNG);
        withMimeType("au", TYPE_AUDIO_BASIC);
        withMimeType("snd", TYPE_AUDIO_BASIC);
        withMimeType("wav", TYPE_AUDIO_X_WAV);
        withMimeType("mov", TYPE_VIDEO_QUICKTIME);
        withMimeType("qt", TYPE_VIDEO_QUICKTIME);
        withMimeType("mpeg", TYPE_VIDEO_MPEG);
        withMimeType("mpg", TYPE_VIDEO_MPEG);
        withMimeType("mpe", TYPE_VIDEO_MPEG);
        withMimeType("abs", TYPE_VIDEO_MPEG);
        withMimeType("doc", TYPE_APPLICATION_MSWORD);
        withMimeType("xls", TYPE_APPLICATION_VND_MSEXCEL);
        withMimeType("eps", TYPE_APPLICATION_POSTSCRIPT);
        withMimeType("ai", TYPE_APPLICATION_POSTSCRIPT);
        withMimeType("ps", TYPE_APPLICATION_POSTSCRIPT);
        withMimeType("pdf", TYPE_APPLICATION_PDF);
        withMimeType("exe", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("dll", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("class", TYPE_APPLICATION_OCTET_STREAM);
        withMimeType("jar", TYPE_APPLICATION_JAVA_ARCHIVE);

    }

    public static void withMimeType(String ext, String mimeType) {
        mimeTypes.put(ext.toLowerCase(), mimeType);
    }

    public static String getMimeType(String ext) {
        return mimeTypes.get(ext.toLowerCase());
    }
}

   