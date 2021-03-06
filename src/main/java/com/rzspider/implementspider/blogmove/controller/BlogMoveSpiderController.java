package com.rzspider.implementspider.blogmove.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.rzspider.common.constant.CommonSymbolicConstant;
import com.rzspider.common.constant.WebSocketConstants;
import com.rzspider.common.constant.project.BlogConstant;
import com.rzspider.common.utils.DateUtils;
import com.rzspider.common.utils.MapUtils;
import com.rzspider.common.utils.security.ShiroUtils;
import com.rzspider.common.utils.spring.SpringUtils;
import com.rzspider.framework.websocket.service.WebSocketPushHandler;
import com.rzspider.implementspider.blogmove.service.BlogMoveArticleService;
import com.rzspider.implementspider.blogmove.service.BlogMoveWordArticleService;
import com.rzspider.implementspider.blogmove.utils.BlogMoveCommonUtils;
import com.rzspider.implementspider.blogmove.utils.BlogMoveTouTiaoUtils;
import com.rzspider.project.blog.blogcontent.domain.Blogcontent;
import com.rzspider.project.blog.blogcontent.domain.Blogmove;
import com.rzspider.project.blog.blogcontent.service.BlogmoveServiceImpl;
import com.rzspider.project.blog.blogcontent.service.IBlogcontentService;
import com.rzspider.project.blog.blogcontent.service.IBlogmoveService;
import com.rzspider.project.blog.blogcontent.utils.BlogUtil;
import com.rzspider.project.blog.blogset.domain.Blogset;
import com.rzspider.project.blog.blogset.service.IBlogsetService;

/**
 * @author ricozhou
 * @date Oct 17, 2018 12:10:48 PM
 * @Desc
 */
public class BlogMoveSpiderController {
	BlogMoveArticleService blogMoveArticleService = new BlogMoveArticleService();
	BlogMoveWordArticleService blogMoveWordArticleService = new BlogMoveWordArticleService();
	IBlogcontentService blogcontentService = (IBlogcontentService) SpringUtils.getBean(IBlogcontentService.class);
	IBlogmoveService blogmoveService = (IBlogmoveService) SpringUtils.getBean(IBlogmoveService.class);
	IBlogsetService blogsetService = (IBlogsetService) SpringUtils.getBean(IBlogsetService.class);
	public int num = 0;
	public StringBuilder sbmsg = new StringBuilder();

	/**
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 * @date Oct 17, 2018 12:11:53 PM
	 * @Desc ????????????????????????
	 */
	public boolean blogMoveController(Blogmove blogMove) {
		String oneUrl;
		// ????????????
		if (blogMove == null) {
			return false;
		}
		// ???????????????????????????
		Blogset blogset = blogsetService.selectBlogsetWaterMarkMsgById(1);
		blogMove.setBlogset(blogset);

		if (blogMove.getMoveMode() == 0) {
			blogMoveArticleController(blogMove);

		} else if (blogMove.getMoveMode() == 1) {
			blogMoveArticleController(blogMove);
		} else if (blogMove.getMoveMode() == 2) {
			// ??????word
			blogMoveWordController(blogMove);
		} else if (blogMove.getMoveMode() == 9) {

		}
		return false;
	}

	/**
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws FailingHttpStatusCodeException
	 * @throws InterruptedException
	 * @date Oct 17, 2018 12:11:53 PM
	 * @Desc CSDN????????????????????????
	 */
	public boolean blogMoveArticleController(Blogmove blogMove) {
		String loginName = ShiroUtils.getLoginName();
		String statusMsg;

		String oneUrl = BlogMoveCommonUtils.getBlogMoveArticleListUrl(blogMove);

		// ????????????
		if (blogMove == null) {
			return false;
		}
		// ????????????????????????url list
		List<String> urlList = new ArrayList<String>();
		int pageNum = BlogMoveCommonUtils.getBlogMoveArticlePageNum(blogMove);
		String webName = blogMove.getMoveWebsiteId();
		statusMsg = "-->> ????????????" + webName + "????????????URL...";
		WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
				new TextMessage(statusMsg));
		sbmsg.append(blogMove.getMoveMessage() + "\n\r" + statusMsg);
		try {
			if (blogMove.getMoveMode() == 0) {
				String max_behot_time = "0";
				for (int i = 1; i < pageNum + 1; i++) {
					if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_CSDN.equals(blogMove.getMoveWebsiteId())) {
						blogMoveArticleService.getCSDNArticleUrlList(blogMove, oneUrl + i, urlList);
					} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_CNBLOG.equals(blogMove.getMoveWebsiteId())) {
						blogMoveArticleService.getCnBlogArticleUrlList(blogMove, oneUrl + i, urlList);
					} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_TOUTIAO.equals(blogMove.getMoveWebsiteId())) {
						// ???????????????????????????
						// ???????????????????????????as???cp???_signature???????????????????????????????????????????????????
						oneUrl = BlogMoveTouTiaoUtils.getTouTiaoListUrl(blogMove, i, max_behot_time);
						max_behot_time = blogMoveArticleService.getTouTiaoArticleUrlList(blogMove, oneUrl, urlList);
					} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_JIANSHU.equals(blogMove.getMoveWebsiteId())) {
						blogMoveArticleService.getJianShuArticleUrlList(blogMove, oneUrl + i, urlList);
					} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_OSCHINA.equals(blogMove.getMoveWebsiteId())) {
						blogMoveArticleService.getOsChinaArticleUrlList(blogMove, String.format(oneUrl, i), urlList);
					}

				}
			} else if (blogMove.getMoveMode() == 1) {
				urlList.clear();
				urlList.add(blogMove.getMoveWebsiteUrl());
			}

			statusMsg = "-->> " + webName + "????????????URL????????????...";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			// ??????url????????????????????????????????????
			if (urlList == null || urlList.size() < 1) {
				statusMsg = "-->> ?????????" + webName + "??????...";
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
						new TextMessage(statusMsg));
				sbmsg.append("\n\r" + statusMsg);
				statusMsg = "over";
				WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
						new TextMessage(statusMsg));
				blogMove.setMoveMessage(sbmsg.toString());
				blogMove.setMoveSuccess(0);
				blogMove.setMoveStopMode(0);
				blogMove.setMoveSuccessNum(num);
				blogmoveService.updateBlogmove(blogMove);
				// ???map???????????????
				MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadMap, loginName);
				MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadBMCMap, loginName);
				return true;
			}
			// ?????????????????????
			List<Blogcontent> bList = null;
			if (blogMove.getMoveRemoveRepeat() == 0) {
				bList = blogcontentService.selectBlogcontentListWithoutContent(null);
			}

			// ????????????
			Blogcontent blogcontent = new Blogcontent();

			for (String url : urlList) {
				if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_CSDN.equals(blogMove.getMoveWebsiteId())) {
					blogcontent = blogMoveArticleService.getCSDNArticleMsg(blogMove, url, bList);
				} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_CNBLOG.equals(blogMove.getMoveWebsiteId())) {
					blogcontent = blogMoveArticleService.getCnBlogArticleMsg(blogMove, url, bList);
				} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_TOUTIAO.equals(blogMove.getMoveWebsiteId())) {
					blogcontent = blogMoveArticleService.getTouTiaoArticleMsg(blogMove, url, bList);
				} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_JIANSHU.equals(blogMove.getMoveWebsiteId())) {
					blogcontent = blogMoveArticleService.getJianShuArticleMsg(blogMove, url, bList);
				} else if (BlogConstant.BLOG_BLOGMOVE_WEBSITE_NAME_OSCHINA.equals(blogMove.getMoveWebsiteId())) {
					blogcontent = blogMoveArticleService.getOsChinaArticleMsg(blogMove, url, bList);
				}

				// ???????????????
				if (blogcontent != null) {
					statusMsg = String.format(
							"-->> ????????????" + webName + "?????? -- <a href=\"%s\" target=\"_blank\">%s</a> -- %s -- %s", url,
							blogcontent.getTitle(),
							DateUtils.format(blogcontent.getGtmCreate(), DateUtils.YYYY_MM_DD_HH_MM_SS),
							blogcontent.getAuthor());
					WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
							new TextMessage(statusMsg));
					sbmsg.append("\n\r" + statusMsg);
					blogcontentService.insertBlogcontent(blogcontent);
					statusMsg = String.format("-->> ????????????????????? -- <a href=\"%s\" target=\"_blank\">%s</a> -- %s -- %s",
							url, blogcontent.getTitle(),
							DateUtils.format(blogcontent.getGtmCreate(), DateUtils.YYYY_MM_DD_HH_MM_SS),
							blogcontent.getAuthor());
					WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
							new TextMessage(statusMsg));
					sbmsg.append("\n\r" + statusMsg);
					num++;
				}
				// ????????????
				Thread.sleep(1000);
			}
			statusMsg = "??????????????????";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			blogMove.setMoveSuccess(0);
			blogMove.setMoveStopMode(0);
			blogMove.setMoveSuccessNum(num);
		} catch (Exception e) {
			e.printStackTrace();
			statusMsg = "??????????????????";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			blogMove.setMoveSuccess(1);
			blogMove.setMoveStopMode(0);
			blogMove.setMoveSuccessNum(num);
		}
		statusMsg = "over";
		WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
				new TextMessage(statusMsg));
		blogMove.setMoveMessage(blogMove.getMoveMessage() + "\n\r" + sbmsg.toString());
		// ????????????
		blogmoveService.updateBlogmove(blogMove);
		// ???map???????????????
		MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadMap, loginName);
		MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadBMCMap, loginName);
		return true;
	}

	/**
	 * @date Oct 29, 2018 2:34:48 PM
	 * @Desc ??????word
	 * @param blogMove
	 */
	private boolean blogMoveWordController(Blogmove blogMove) {
		String loginName = ShiroUtils.getLoginName();
		String statusMsg;

		// ????????????
		if (blogMove == null) {
			return false;
		}
		statusMsg = "-->> ????????????wWORD??????...";
		WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
				new TextMessage(statusMsg));
		sbmsg.append(blogMove.getMoveMessage() + "\n\r" + statusMsg);

		if (blogMove.getMoveFileNames() == null
				|| CommonSymbolicConstant.EMPTY_STRING.equals(blogMove.getMoveFileNames())
				|| blogMove.getMoveFileNames().split(CommonSymbolicConstant.COMMA).length < 1) {
			statusMsg = "-->> ?????????WORD??????...";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			statusMsg = "over";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			blogMove.setMoveMessage(sbmsg.toString());
			blogMove.setMoveSuccess(0);
			blogMove.setMoveStopMode(0);
			blogMove.setMoveSuccessNum(num);
			blogmoveService.updateBlogmove(blogMove);
			// ???map???????????????
			MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadMap, loginName);
			MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadBMCMap, loginName);
			return true;
		}

		// ?????????????????????
		List<Blogcontent> bList = null;
		if (blogMove.getMoveRemoveRepeat() == 0) {
			bList = blogcontentService.selectBlogcontentListWithoutContent(null);
		}
		try {
			String[] fileNames = blogMove.getMoveFileNames().split(CommonSymbolicConstant.COMMA);
			String[] fileONames = blogMove.getMoveFileONames().split(CommonSymbolicConstant.COMMA);
			Blogcontent blogcontent;
			for (int i = 0; i < fileNames.length; i++) {
				// ??????word?????????html?????????????????????

				blogcontent = BlogMoveWordArticleService.getWordArticleMsg(blogMove, fileNames[i], fileONames[i],
						bList);

				// ???????????????
				if (blogcontent != null) {
					statusMsg = String.format("-->> ????????????WORD??????  --" + fileONames[i] + " ");
					WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
							new TextMessage(statusMsg));
					sbmsg.append("\n\r" + statusMsg);
					blogcontentService.insertBlogcontent(blogcontent);
					statusMsg = String.format("-->> ?????????????????????  --" + fileONames[i] + " ");
					WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
							new TextMessage(statusMsg));
					sbmsg.append("\n\r" + statusMsg);
					num++;
				}
				// ????????????
				Thread.sleep(1000);
			}
			statusMsg = "??????????????????";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			blogMove.setMoveSuccess(0);
			blogMove.setMoveStopMode(0);
			blogMove.setMoveSuccessNum(num);
		} catch (Exception e) {
			e.printStackTrace();
			statusMsg = "??????WORD????????????";
			WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
					new TextMessage(statusMsg));
			sbmsg.append("\n\r" + statusMsg);
			blogMove.setMoveSuccess(1);
			blogMove.setMoveStopMode(0);
			blogMove.setMoveSuccessNum(num);
		}
		statusMsg = "over";
		WebSocketPushHandler.sendMessageToUser(WebSocketConstants.WEBSOCKET_PARAMS_BLOGMOVEID, loginName,
				new TextMessage(statusMsg));
		blogMove.setMoveMessage(blogMove.getMoveMessage() + "\n\r" + sbmsg.toString());
		// ????????????
		blogmoveService.updateBlogmove(blogMove);
		// ???map???????????????
		MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadMap, loginName);
		MapUtils.removeObjectFromMap(BlogmoveServiceImpl.blogMoveThreadBMCMap, loginName);
		return true;

	}

	public int getNum() {
		return num;
	}

	public String getSbmsg() {
		return sbmsg.toString();
	}

}
