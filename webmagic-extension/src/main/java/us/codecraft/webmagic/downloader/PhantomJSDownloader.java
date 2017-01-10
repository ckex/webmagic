package us.codecraft.webmagic.downloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.annotation.ThreadSafe;
import org.apache.log4j.helpers.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.selector.PlainText;

import java.io.*;

/**
 * this downloader is used to download pages which need to render the javascript
 *
 * @author dolphineor@gmail.com
 * @version 0.5.3
 */
@ThreadSafe
public class PhantomJSDownloader extends AbstractDownloader {

    private static Logger logger = LoggerFactory.getLogger(PhantomJSDownloader.class);
    private static String crawlJsPath;
    private static String phantomJsCommand = "phantomjs"; // default

    private int retryNum;
    private int threadNum;

    public PhantomJSDownloader() {
        this.initPhantomjsCrawlPath(null);
    }
    
    public PhantomJSDownloader(String crawljs,String phantomJsCommand) {
        this.initPhantomjsCrawlPath(crawljs);
        PhantomJSDownloader.phantomJsCommand = phantomJsCommand;
    }
    
    /**
       * 添加新的构造函数，支持phantomjs自定义命令
       * 
       * example: 
       *    phantomjs.exe 支持windows环境
       *    phantomjs --ignore-ssl-errors=yes 忽略抓取地址是https时的一些错误
       *    /usr/local/bin/phantomjs 命令的绝对路径，避免因系统环境变量引起的IOException
       *   
       * @param phantomJsCommand phantomJsCommand
       */
    public PhantomJSDownloader(String phantomJsCommand) {
        this.initPhantomjsCrawlPath(null);
        PhantomJSDownloader.phantomJsCommand = phantomJsCommand;
    }
    
	private void initPhantomjsCrawlPath(String crawljs) {
		if (StringUtils.isBlank(crawljs)) {
			String p = Class.class.getClass().getResource("/").getPath();
			// p = this.getClass().getResource("/").getPath();
			File file = new File(p);
			String path = file.getPath() + System.getProperty("file.separator") + "crawl.js ";
			System.out.println("file path=" + path);
			PhantomJSDownloader.crawlJsPath = path;
		} else {
			PhantomJSDownloader.crawlJsPath = crawljs;
		}
	}

    @Override
    public Page download(Request request, Task task) {
        if (logger.isInfoEnabled()) {
            logger.info("downloading page: " + request.getUrl());
        }
        String content = getPage(request);
        if (content.contains("HTTP request failed")) {
            for (int i = 1; i <= getRetryNum(); i++) {
                content = getPage(request);
                if (!content.contains("HTTP request failed")) {
                    break;
                }
            }
            if (content.contains("HTTP request failed")) {
                //when failed
                Page page = new Page();
                page.setRequest(request);
                return page;
            }
        }

        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(200);
        return page;
    }

    @Override
    public void setThread(int threadNum) {
        this.threadNum = threadNum;
    }

    protected String getPage(Request request) {
        try {
            String url = request.getUrl();
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(phantomJsCommand + " " + crawlJsPath + url);
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuffer.append(line).append("\n");
            }
            return stringBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getRetryNum() {
        return retryNum;
    }

    public PhantomJSDownloader setRetryNum(int retryNum) {
        this.retryNum = retryNum;
        return this;
    }
}
