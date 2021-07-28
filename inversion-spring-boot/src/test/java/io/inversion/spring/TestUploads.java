package io.inversion.spring;

import io.inversion.*;
import io.inversion.spring.main.InversionMain;
//import io.inversion.utils.StreamBuffer;
import io.inversion.utils.Utils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class TestUploads {

//    public static void main(String[] args)throws Exception{
//        //multipart.max-file-size=100MB
//        //multipart.max-request-size=100MB
//
//        //System.setProperty("multipart.max-file-size", "1024");
//        //System.setProperty("multipart.max-file-size", "1024");
//
//        //System.setProperty("spring.servlet.multipart.max-request-size", "1024");
//        //System.setProperty("spring.servlet.multipart.max-file-size", "1024");
//
//        InversionMain.run(new Api("testme").withEndpoint("*", "*", new Action() {
//            public void run(Request req, Response res) throws ApiException {
//                String               text    = "";
//                List<Request.Upload> uploads = req.getUploads();
//                for(Request.Upload upload : uploads){
//                    text += "Upload: " + upload.getFileName() + ":" + upload.getFileSize() + "\r\n";
//                }
//                res.withText(text);
//            }
//        }));
//
//
//        String response = upload("http://localhost:8080/testme", 5000000);
//
//        Thread.sleep(2000);
//        InversionMain.exit();
//    }

//    public static String upload(String url, long size)throws Exception{
//
//        File file = null;
//        try {
//
//            file = File.createTempFile("temp", ".temp");
//
//            System.out.println("CREATING FILE: " + file.getCanonicalPath());
//
//            file.deleteOnExit();
//            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
//
//            for(int i=0; i<size; i++){
//                out.write(1);
//            }
//            out.flush();
//            out.close();
//
//
//            CloseableHttpClient httpclient = HttpClients.createDefault();
//
//
//            // build multipart upload request
//            HttpEntity data = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
//                    .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
//                    .addTextBody("text", "Uploading file", ContentType.DEFAULT_BINARY).build();
//
//            // build http request and assign multipart upload data
//            HttpUriRequest request = RequestBuilder.post(url).setEntity(data).build();
//
//            CloseableHttpResponse response = httpclient.execute(request);
//
//            HttpEntity   entity = response.getEntity();
//            StreamBuffer buff   = new StreamBuffer();
//            Utils.pipe(entity.getContent(), buff);
//            System.out.println(buff.getBufferSize());
//
//            return null;
//        }
//        finally{
//            file.delete();
//        }
//    }

}
