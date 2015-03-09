/*
 * HttpConnector.java - europeana4j
 * (C) 2011 Digibis S.L.
 */
package eu.europeana.api.client.connection;

import java.io.*;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * A HttpConnector is a class encapsulating simple HTTP access.
 *
 * @author Andres Viedma Pelez
 * @author Sergiu Gordea
 */
public class HttpConnector {

    private static final int CONNECTION_RETRIES = 3;
    private static final int TIMEOUT_CONNECTION = 40000;
    private static final int STATUS_OK_START = 200;
    private static final int STATUS_OK_END = 299;
    private static final String ENCODING = "UTF-8";
    private HttpClient httpClient = null;

    private static final Log log = LogFactory.getLog(HttpConnector.class);

    public String getURLContent(String url) throws IOException {
        HttpClient client = this.getHttpClient(CONNECTION_RETRIES, TIMEOUT_CONNECTION);
        HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return String.valueOf(result);

        } finally {
            getRequest.releaseConnection();
        }
    }

    public boolean writeURLContent(String url, OutputStream out) throws IOException {
        return writeURLContent(url, out, null);
    }

    public boolean writeURLContent(String url, OutputStream out, String requiredMime) throws IOException {
        HttpClient client = this.getHttpClient(CONNECTION_RETRIES, TIMEOUT_CONNECTION);
        HttpGet getMethod = new HttpGet(url);
        try {
            HttpResponse response = client.execute(getMethod);

            Header tipoMimeHead = response.getFirstHeader("Content-Type");
            String tipoMimeResp = "";
            if (tipoMimeHead != null) {
                tipoMimeResp = tipoMimeHead.getValue();
            }

            if (response.getStatusLine().getStatusCode() >= STATUS_OK_START && response.getStatusLine().getStatusCode() <= STATUS_OK_END
                    && ((requiredMime == null) || ((tipoMimeResp != null) && tipoMimeResp.contains(requiredMime)))) {
                InputStream in = response.getEntity().getContent();

                // Copy input stream to output stream
                byte[] b = new byte[4 * 1024];
                int read;
                while ((read = in.read(b)) != -1) {
                    out.write(b, 0, read);
                }

                getMethod.releaseConnection();
                return true;
            } else {
                return false;
            }

        } finally {
            getMethod.releaseConnection();
        }
    }

    public boolean silentWriteURLContent(String url, OutputStream out, String mimeType) {
        try {
            return this.writeURLContent(url, out, mimeType);

        } catch (Exception e) {
            log.debug("Exception occured when copying thumbnail from url: " + url, e);
            return false;
        }
    }

    public boolean checkURLContent(String url, String requiredMime) {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b)
                    throws IOException {
            }
        };

        boolean bOk = this.silentWriteURLContent(url, out, requiredMime);
        return bOk;
    }

    private HttpClient getHttpClient(int connectionRetry, int conectionTimeout) {
        if (this.httpClient == null) {
            HttpClient client = HttpClientBuilder.create().build();

//            //TODO: write english code comments
//            //Se configura el n�mero de reintentos
//            client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
//                    new DefaultHttpMethodRetryHandler(connectionRetry, false));
//
//
//            //TODO: write english code comments
//            //Se comprueban las propiedades proxy del sistema. Si est�n rellenas, se rellena
//            String proxyHost = System.getProperty("http.proxyHost");
//            if ((proxyHost != null) && (proxyHost.length() > 0)) {
//                String proxyPortSrt = System.getProperty("http.proxyPort");
//                if (proxyPortSrt == null) {
//                    proxyPortSrt = "8080";
//                }
//                int proxyPort = Integer.parseInt(proxyPortSrt);
//
//                client.getHostConfiguration().setProxy(proxyHost, proxyPort);
//            }
//
//            //TODO: write english code comments
//            //Se configura el timeout de la conexion. Primero se intenta asignar los par�metros
//            //pasados. Si est�n vac�os, se pone el par�metro por defecto
//            boolean bTimeout = false;
//            String connectTimeOut = System.getProperty("sun.net.client.defaultConnectTimeout");
//            if ((connectTimeOut != null) && (connectTimeOut.length() > 0)) {
//                client.getParams().setIntParameter("sun.net.client.defaultConnectTimeout", Integer.parseInt(connectTimeOut));
//                bTimeout = true;
//            }
//            String readTimeOut = System.getProperty("sun.net.client.defaultReadTimeout");
//            if ((readTimeOut != null) && (readTimeOut.length() > 0)) {
//                client.getParams().setIntParameter("sun.net.client.defaultReadTimeout", Integer.parseInt(readTimeOut));
//                bTimeout = true;
//            }
//            if (!bTimeout) {
//                client.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, conectionTimeout);
//            }

            this.httpClient = client;
        }
        return this.httpClient;
    }

    public String getURLContent(String url, String jsonParamName, String jsonParamValue) throws IOException {
        HttpClient client = this.getHttpClient(CONNECTION_RETRIES, TIMEOUT_CONNECTION);
        HttpPost post = new HttpPost(url);
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair(jsonParamName, jsonParamValue));
        post.setEntity(new UrlEncodedFormEntity(postParameters));

        try {
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() >= STATUS_OK_START && response.getStatusLine().getStatusCode() <= STATUS_OK_END) {
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                return String.valueOf(result);
            } else {
                return null;
            }

        } finally {
            post.releaseConnection();
        }
    }
}
