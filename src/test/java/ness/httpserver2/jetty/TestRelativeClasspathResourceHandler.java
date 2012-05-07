package ness.httpserver2.jetty;

import io.trumpet.httpclient.HttpClient;
import io.trumpet.httpclient.HttpClientResponse;
import io.trumpet.httpclient.HttpClientResponseHandler;
import io.trumpet.httpclient.response.ContentResponseHandler;
import io.trumpet.httpclient.response.StringContentConverter;
import io.trumpet.servlet.test.LocalHttpService;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.server.Handler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.nesscomputing.testing.lessio.AllowNetworkAccess;
import com.nesscomputing.testing.lessio.AllowNetworkListen;


@AllowNetworkListen(ports= {0})
@AllowNetworkAccess(endpoints= {"127.0.0.1:*"})
public class TestRelativeClasspathResourceHandler
{
    private Handler testHandler = null;
    private LocalHttpService localHttpService = null;

    private HttpClient httpClient = null;

    private String baseUri;

    @Before
    public void setup()
    {
        Assert.assertNull(localHttpService);
        Assert.assertNull(testHandler);

        final String prefix = "/foobar";
        testHandler = new ClasspathResourceHandler(prefix, "/test-resources");
        localHttpService = LocalHttpService.forHandler(testHandler);
        localHttpService.start();

        httpClient = new HttpClient().start();

        baseUri = "http://" + localHttpService.getHost() + ":" + localHttpService.getPort() + prefix;

    }

    @After
    public void teardown()
    {
        Assert.assertNotNull(localHttpService);
        Assert.assertNotNull(testHandler);

        localHttpService.stop();
        localHttpService = null;
        testHandler = null;

        Assert.assertNotNull(httpClient);
        httpClient.close();
        httpClient = null;

        baseUri = null;
    }

    @Test
    public void testSimpleGet() throws Exception
    {
        final String content = httpClient.get(baseUri + "/simple-content.txt", StringContentConverter.DEFAULT_RESPONSE_HANDLER).perform();

        Assert.assertEquals("this is simple content for a simple test\n", content);
    }

    @Test
    public void testSimpleHead() throws Exception
    {
        final String content = httpClient.head(baseUri + "/simple-content.txt", StringContentConverter.DEFAULT_RESPONSE_HANDLER).perform();

        Assert.assertEquals("", content);
    }

    @Test
    public void testSimplePost() throws Exception
    {
        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(405, response.getStatusCode());
                return null;
            }
        });

        final String content = httpClient.post(baseUri + "/simple-content.txt", responseHandler).perform();

        Assert.assertEquals(null, content);
    }


    @Test
    public void testMissing() throws Exception
    {
        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(404, response.getStatusCode());
                return null;
            }
        });

        final String content = httpClient.get(baseUri + "/missing", responseHandler).perform();

        Assert.assertNull(content);
    }

    @Test
    public void testIfModified() throws Exception
    {
        final AtomicReference<String> holder = new AtomicReference<String>(null);
        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                holder.set(response.getHeader("Last-Modified"));
                return super.convert(response, inputStream);
            }
        });

        final String content = httpClient.get(baseUri + "/simple-content.txt", responseHandler).perform();

        Assert.assertNotNull(holder.get());
        Assert.assertEquals("this is simple content for a simple test\n", content);

        final HttpClientResponseHandler<String> responseHandler2 = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(304, response.getStatusCode());
                return null;
            }
        });

        final String content2 = httpClient.get(baseUri + "/simple-content.txt", responseHandler2).addHeader("If-Modified-Since", holder.get()).perform();

        Assert.assertNull(content2);
    }

    @Test
    public void testWelcomeFile1() throws Exception
    {
        final String content = httpClient.get(baseUri + "/", StringContentConverter.DEFAULT_RESPONSE_HANDLER).perform();
        Assert.assertEquals("the welcome file\n", content);
    }

    @Test
    public void testWelcomeFile2() throws Exception
    {
        final String content = httpClient.get(baseUri, StringContentConverter.DEFAULT_RESPONSE_HANDLER).perform();
        Assert.assertEquals("the welcome file\n", content);
    }

    @Test
    public void testWelcomeFile3() throws Exception
    {
        final String content = httpClient.get(baseUri + "/index.html", StringContentConverter.DEFAULT_RESPONSE_HANDLER).perform();
        Assert.assertEquals("the welcome file\n", content);
    }

    @Test
    public void testExtendPrefix() throws Exception
    {
        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(404, response.getStatusCode());
                return null;
            }
        });

        final String content = httpClient.get(baseUri + "bar/index.html", responseHandler).perform();

        Assert.assertNull(content);
    }

    @Test
    public void testRoot() throws Exception
    {
        final String rootUri = "http://" + localHttpService.getHost() + ":" + localHttpService.getPort() + "/index.html";

        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(404, response.getStatusCode());
                return null;
            }
        });

        final String content = httpClient.get(rootUri, responseHandler).perform();

        Assert.assertNull(content);
    }

    @Test
    public void testOtherPrefix() throws Exception
    {
        final String rootUri = "http://" + localHttpService.getHost() + ":" + localHttpService.getPort() + "/bla/blo/index.html";

        final HttpClientResponseHandler<String> responseHandler = ContentResponseHandler.forConverter(new StringContentConverter() {
            @Override
            public String convert(final HttpClientResponse response, final InputStream inputStream) throws IOException
            {
                Assert.assertEquals(404, response.getStatusCode());
                return null;
            }
        });

        final String content = httpClient.get(rootUri, responseHandler).perform();

        Assert.assertNull(content);
    }



}