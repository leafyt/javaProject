package com.rzspider.implementspider.blogmove.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.socket.TextMessage;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.rzspider.common.constant.WebSocketConstants;
import com.rzspider.common.constant.project.BlogConstant;
import com.rzspider.common.utils.DateUtils;
import com.rzspider.common.utils.security.ShiroUtils;
import com.rzspider.framework.websocket.service.WebSocketPushHandler;
import com.rzspider.implementspider.blogmove.utils.BlogMoveCSDNUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveCnBlogUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveCommonUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveJianShuUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveOsChinaUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveTouTiaoUtils;
import com.rzspider.project.blog.blogcontent.domain.Blogcontent;
import com.rzspider.project.blog.blogcontent.domain.Blogmove;

/**
 * @author ricozhou
 * @date Oct 17, 2018 12:28:21 PM
 * @Desc
 */
public class BlogMoveArticleService {

	/**
	 * @date Oct 17, 2018 12:30:46 PM
	 * @Desc
	 * @param blogMove
	 * @param oneUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public void getCSDNArticleUrlList(Blogmove blogMove, String oneUrl, List<String> urlList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(oneUrl);
		// System.out.println(page.asXml());
		Document doc = Jsoup.parse(page.asXml());
		Element pageMsg22 = doc.select("div.article-list").first();
		if (pageMsg22 == null) {
			return;
		}
		Elements pageMsg = pageMsg22.select("div.article-item-box");
		Element linkNode;
		for (Element e : pageMsg) {
			linkNode = e.select("h4 a").first();
			// ????????????????????????bloglist????????????????????????https://blog.csdn.net/yoyo_liyy/article/details/82762601
			if (linkNode.attr("href").contains(blogMove.getMoveUserId())) {
				if (urlList.size() < blogMove.getMoveNum()) {
					urlList.add(linkNode.attr("href"));
				} else {
					break;
				}
			}
		}
		return;
	}

	/**
	 * @date Oct 17, 2018 12:46:52 PM
	 * @Desc ??????????????????
	 * @param blogMove
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public Blogcontent getCSDNArticleMsg(Blogmove blogMove, String url, List<Blogcontent> bList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Blogcontent blogcontent = new Blogcontent();
		blogcontent.setArticleSource(blogMove.getMoveWebsiteId());
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.EDGE);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(url);

		Document doc = Jsoup.parse(page.asXml());
		// ????????????
		String title = BlogMoveCSDNUtils.getCSDNArticleTitle(doc);
		// ??????????????????
		if (blogMove.getMoveRemoveRepeat() == 0) {
			// ??????????????????
			if (BlogMoveCommonUtils.articleRepeat(bList, title)) {
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID,
						ShiroUtils.getLoginName(), new TextMessage(
								String.format("-->> ??????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> ", url, title)));
				return null;
			}
		}
		blogcontent.setTitle(title);
		// ????????????
		blogcontent.setAuthor(BlogMoveCSDNUtils.getCSDNArticleAuthor(doc));
		// ????????????
		if (blogMove.getMoveUseOriginalTime() == 0) {
			blogcontent.setGtmCreate(BlogMoveCSDNUtils.getCSDNArticleTime(doc));
		} else {
			blogcontent.setGtmCreate(new Date());
		}
		blogcontent.setGtmModified(new Date());
		// ????????????
		blogcontent.setType(BlogMoveCSDNUtils.getCSDNArticleType(doc,blogMove.getMoveArticleType()));
		// ????????????
		blogcontent.setContent(BlogMoveCSDNUtils.getCSDNArticleContent(doc, blogMove, blogcontent));

		// ????????????
		blogcontent.setStatus(blogMove.getMoveBlogStatus());
		blogcontent.setBlogColumnName(blogMove.getMoveColumn());
		// ????????????
		blogcontent.setArticleEditor(blogMove.getMoveArticleEditor());
		blogcontent.setShowId(DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSSSSS));
		blogcontent.setAllowComment(0);
		blogcontent.setAllowPing(0);
		blogcontent.setAllowDownload(0);
		blogcontent.setShowIntroduction(1);
		blogcontent.setIntroduction("");
		blogcontent.setPrivateArticle(1);

		return blogcontent;
	}

	/**
	 * @date Oct 17, 2018 12:30:46 PM
	 * @Desc
	 * @param blogMove
	 * @param oneUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public void getJianShuArticleUrlList(Blogmove blogMove, String oneUrl, List<String> urlList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(oneUrl);
		// System.out.println(page.asXml());
		Document doc = Jsoup.parse(page.asXml());
		Element pageMsg22 = doc.select("ul.note-list").first();
		if (pageMsg22 == null) {
			return;
		}
		Elements pageMsg = pageMsg22.select("div.content");
		Element linkNode;
		for (Element e : pageMsg) {
			linkNode = e.select("a.title").first();
			if (linkNode == null) {
				continue;
			}
			if (urlList.size() < blogMove.getMoveNum()) {
				urlList.add(BlogConstant.BLOG_BLOGMOVE_WEBSITE_BASEURL_JIANSHU + linkNode.attr("href"));
			} else {
				break;
			}
		}
		return;
	}

	/**
	 * @date Oct 17, 2018 12:46:52 PM
	 * @Desc ??????????????????
	 * @param blogMove
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public Blogcontent getJianShuArticleMsg(Blogmove blogMove, String url, List<Blogcontent> bList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Blogcontent blogcontent = new Blogcontent();
		blogcontent.setArticleSource(blogMove.getMoveWebsiteId());
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(url);

		Document doc = Jsoup.parse(page.asXml());
		// ????????????
		String title = BlogMoveJianShuUtils.getJianShuArticleTitle(doc);
		// ??????????????????
		if (blogMove.getMoveRemoveRepeat() == 0) {
			// ??????????????????
			if (BlogMoveCommonUtils.articleRepeat(bList, title)) {
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID,
						ShiroUtils.getLoginName(), new TextMessage(
								String.format("-->> ??????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> ", url, title)));
				return null;
			}
		}
		blogcontent.setTitle(title);
		// ????????????
		blogcontent.setAuthor(BlogMoveJianShuUtils.getJianShuArticleAuthor(doc));
		// ????????????
		if (blogMove.getMoveUseOriginalTime() == 0) {
			blogcontent.setGtmCreate(BlogMoveJianShuUtils.getJianShuArticleTime(doc));
		} else {
			blogcontent.setGtmCreate(new Date());
		}
		blogcontent.setGtmModified(new Date());
		// ????????????
		blogcontent.setType(BlogMoveJianShuUtils.getJianShuArticleType(doc,blogMove.getMoveArticleType()));
		// ????????????
		blogcontent.setContent(BlogMoveJianShuUtils.getJianShuArticleContent(doc, blogMove, blogcontent));

		// ????????????
		blogcontent.setStatus(blogMove.getMoveBlogStatus());
		blogcontent.setBlogColumnName(blogMove.getMoveColumn());
		// ????????????
		blogcontent.setArticleEditor(blogMove.getMoveArticleEditor());
		blogcontent.setShowId(DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSSSSS));
		blogcontent.setAllowComment(0);
		blogcontent.setAllowPing(0);
		blogcontent.setAllowDownload(0);
		blogcontent.setShowIntroduction(1);
		blogcontent.setIntroduction("");
		blogcontent.setPrivateArticle(1);

		return blogcontent;
	}

	/**
	 * @date Oct 17, 2018 12:30:46 PM
	 * @Desc
	 * @param blogMove
	 * @param oneUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public void getOsChinaArticleUrlList(Blogmove blogMove, String oneUrl, List<String> urlList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(oneUrl);
		// System.out.println(page.asXml());
		Document doc = Jsoup.parse(page.asXml());
		Element pageMsg22 = doc.select("div.list-container.space-list-container").first();
		if (pageMsg22 == null) {
			return;
		}
		Elements pageMsg = pageMsg22.select("div.content");
		Element linkNode;
		for (Element e : pageMsg) {
			linkNode = e.select("a.header").first();
			if (linkNode == null) {
				continue;
			}
			if (urlList.size() < blogMove.getMoveNum()) {
				urlList.add(linkNode.attr("href"));
			} else {
				break;
			}
		}
		return;
	}

	/**
	 * @date Oct 17, 2018 12:46:52 PM
	 * @Desc ??????????????????
	 * @param blogMove
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public Blogcontent getOsChinaArticleMsg(Blogmove blogMove, String url, List<Blogcontent> bList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Blogcontent blogcontent = new Blogcontent();
		blogcontent.setArticleSource(blogMove.getMoveWebsiteId());
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(url);

		Document doc = Jsoup.parse(page.asXml());
		// ????????????
		String title = BlogMoveOsChinaUtils.getOsChinaArticleTitle(doc);
		// ??????????????????
		if (blogMove.getMoveRemoveRepeat() == 0) {
			// ??????????????????
			if (BlogMoveCommonUtils.articleRepeat(bList, title)) {
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID,
						ShiroUtils.getLoginName(), new TextMessage(
								String.format("-->> ??????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> ", url, title)));
				return null;
			}
		}
		blogcontent.setTitle(title);
		// ????????????
		blogcontent.setAuthor(BlogMoveOsChinaUtils.getOsChinaArticleAuthor(doc));
		// ????????????
		if (blogMove.getMoveUseOriginalTime() == 0) {
			blogcontent.setGtmCreate(BlogMoveOsChinaUtils.getOsChinaArticleTime(doc));
		} else {
			blogcontent.setGtmCreate(new Date());
		}
		blogcontent.setGtmModified(new Date());
		// ????????????
		blogcontent.setType(BlogMoveOsChinaUtils.getOsChinaArticleType(doc,blogMove.getMoveArticleType()));
		// ????????????
		blogcontent.setContent(BlogMoveOsChinaUtils.getOsChinaArticleContent(doc, blogMove, blogcontent));

		// ????????????
		blogcontent.setStatus(blogMove.getMoveBlogStatus());
		blogcontent.setBlogColumnName(blogMove.getMoveColumn());
		// ????????????
		blogcontent.setArticleEditor(blogMove.getMoveArticleEditor());
		blogcontent.setShowId(DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSSSSS));
		blogcontent.setAllowComment(0);
		blogcontent.setAllowPing(0);
		blogcontent.setAllowDownload(0);
		blogcontent.setShowIntroduction(1);
		blogcontent.setIntroduction("");
		blogcontent.setPrivateArticle(1);

		return blogcontent;
	}

	/**
	 * @date Oct 17, 2018 12:30:46 PM
	 * @Desc
	 * @param blogMove
	 * @param oneUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public void getCnBlogArticleUrlList(Blogmove blogMove, String oneUrl, List<String> urlList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(oneUrl);
		// System.out.println(page.asXml());
		Document doc = Jsoup.parse(page.asXml());
		Elements pageMsg = doc.select("div.postTitle");
		Element linkNode;
		for (Element e : pageMsg) {
			linkNode = e.select("a.postTitle2").first();
			if (linkNode == null) {
				continue;
			}
			if (urlList.size() < blogMove.getMoveNum()) {
				urlList.add(linkNode.attr("href"));
			} else {
				break;
			}
		}
		return;
	}

	/**
	 * @date Oct 17, 2018 12:46:52 PM
	 * @Desc ??????????????????
	 * @param blogMove
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public Blogcontent getCnBlogArticleMsg(Blogmove blogMove, String url, List<Blogcontent> bList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Blogcontent blogcontent = new Blogcontent();
		blogcontent.setArticleSource(blogMove.getMoveWebsiteId());
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(url);

		Document doc = Jsoup.parse(page.asXml());
		// ????????????
		String title = BlogMoveCnBlogUtils.getCnBlogArticleTitle(doc);
		// ??????????????????
		if (blogMove.getMoveRemoveRepeat() == 0) {
			// ??????????????????
			if (BlogMoveCommonUtils.articleRepeat(bList, title)) {
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID,
						ShiroUtils.getLoginName(), new TextMessage(
								String.format("-->> ??????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> ", url, title)));
				return null;
			}
		}
		blogcontent.setTitle(title);
		// ????????????
		blogcontent.setAuthor(BlogMoveCnBlogUtils.getCnBlogArticleAuthor(doc));
		// ????????????
		if (blogMove.getMoveUseOriginalTime() == 0) {
			blogcontent.setGtmCreate(BlogMoveCnBlogUtils.getCnBlogArticleTime(doc));
		} else {
			blogcontent.setGtmCreate(new Date());
		}
		blogcontent.setGtmModified(new Date());
		// ????????????
		blogcontent.setType(BlogMoveCnBlogUtils.getCnBlogArticleType(doc,blogMove.getMoveArticleType()));
		// ????????????
		blogcontent.setContent(BlogMoveCnBlogUtils.getCnBlogArticleContent(doc, blogMove, blogcontent));

		// ????????????
		blogcontent.setStatus(blogMove.getMoveBlogStatus());
		blogcontent.setBlogColumnName(blogMove.getMoveColumn());
		// ????????????
		blogcontent.setArticleEditor(blogMove.getMoveArticleEditor());
		blogcontent.setShowId(DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSSSSS));
		blogcontent.setAllowComment(0);
		blogcontent.setAllowPing(0);
		blogcontent.setAllowDownload(0);
		blogcontent.setShowIntroduction(1);
		blogcontent.setIntroduction("");
		blogcontent.setPrivateArticle(1);

		return blogcontent;
	}

	/**
	 * @date Oct 17, 2018 12:30:46 PM
	 * @Desc
	 * @param blogMove
	 * @param oneUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public String getTouTiaoArticleUrlList(Blogmove blogMove, String oneUrl, List<String> urlList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		String max_behot_time = "0";
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		Map<String, String> additionalHeaders = new HashMap<String, String>();
		additionalHeaders.put("user-agent",
				"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
		WebRequest request = new WebRequest(new URL(oneUrl), HttpMethod.GET);
		request.setAdditionalHeaders(additionalHeaders);
		UnexpectedPage page2 = webClient.getPage(request);
		String result = IOUtils.toString(page2.getInputStream(), StandardCharsets.UTF_8);

		JSONObject json = JSON.parseObject(result);
		JSONArray jsonarray = (JSONArray) json.get("data");
		JSONObject json2 = (JSONObject) json.get("next");
		// ??????????????????????????????
		max_behot_time = json2.getString("max_behot_time");

		// ??????url
		JSONObject jsonObject;
		String url;
		for (Object obj : jsonarray) {
			jsonObject = (JSONObject) obj;
			url = jsonObject.getString("source_url");
			if (url != null) {
				url = url.substring(6, url.length() - 1);
				if (urlList.size() < blogMove.getMoveNum()) {
					urlList.add("https://www.toutiao.com/i" + url);
				} else {
					break;
				}
			} else {
				url = "";
				continue;
			}
		}

		return max_behot_time;
	}

	/**
	 * @date Oct 17, 2018 12:46:52 PM
	 * @Desc ??????????????????
	 * @param blogMove
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 */
	public Blogcontent getTouTiaoArticleMsg(Blogmove blogMove, String url, List<Blogcontent> bList)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Blogcontent blogcontent = new Blogcontent();
		blogcontent.setArticleSource(blogMove.getMoveWebsiteId());
		// ?????????????????????
		// ??????WebClient
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		// ??????css????????????
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		// ??????????????????????????????js?????????????????????
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		// ?????????????????????html
		HtmlPage page = webClient.getPage(url);

		Document doc = Jsoup.parse(page.asXml());
		Elements pageMsg22 = doc.body().getElementsByTag("script");

		String msgJsonString = BlogMoveTouTiaoUtils.getTouTiaoArticleMsgJsonString(pageMsg22);

		String content = null;
		String title = null;
		String author = null;
		Date date = null;
		if (msgJsonString != null && !"".equals(msgJsonString)) {
			System.out.println(msgJsonString);
			// ??????
			JSONObject json = JSON.parseObject(msgJsonString);
			json = (JSONObject) json.get("articleInfo");
			content = json.getString("content");
			// ??????
			content = StringEscapeUtils.unescapeHtml3(content);
			title = json.getString("title");
			json = (JSONObject) json.get("subInfo");
			author = json.getString("source");
			date = DateUtils.formatStringDate(json.getString("time"), DateUtils.YYYY_MM_DD_HH_MM_SS);
			date = date == null ? new Date() : date;
		}

		// ??????????????????
		if (blogMove.getMoveRemoveRepeat() == 0) {
			// ??????????????????
			if (BlogMoveCommonUtils.articleRepeat(bList, title)) {
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID,
						ShiroUtils.getLoginName(), new TextMessage(
								String.format("-->> ??????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> ", url, title)));
				return null;
			}
		}
		blogcontent.setTitle(title);
		// ????????????
		blogcontent.setAuthor(author);
		// ????????????
		if (blogMove.getMoveUseOriginalTime() == 0) {
			blogcontent.setGtmCreate(date);
		} else {
			blogcontent.setGtmCreate(new Date());
		}
		blogcontent.setGtmModified(new Date());
		// ????????????
		blogcontent.setType("??????");
		blogcontent.setType(BlogMoveTouTiaoUtils.getTouTiaoArticleType(msgJsonString,blogMove.getMoveArticleType()));
		// ????????????
		blogcontent.setContent(BlogMoveTouTiaoUtils.getTouTiaoArticleContent(content, blogMove, blogcontent));

		// ????????????
		blogcontent.setStatus(blogMove.getMoveBlogStatus());
		blogcontent.setBlogColumnName(blogMove.getMoveColumn());
		// ????????????
		blogcontent.setArticleEditor(blogMove.getMoveArticleEditor());
		blogcontent.setShowId(DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSSSSS));
		blogcontent.setAllowComment(0);
		blogcontent.setAllowPing(0);
		blogcontent.setAllowDownload(0);
		blogcontent.setShowIntroduction(1);
		blogcontent.setIntroduction("");
		blogcontent.setPrivateArticle(1);

		return blogcontent;
	}

}
