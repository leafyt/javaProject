package com.rzspider.project.commontool.toolrun.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.log.SysoCounter;
import com.rzspider.common.constant.CodingConstant;
import com.rzspider.common.constant.CommonSymbolicConstant;
import com.rzspider.common.constant.FileExtensionConstant;
import com.rzspider.common.constant.FileOtherConstant;
import com.rzspider.common.constant.ReturnMessageConstant;
import com.rzspider.common.utils.ImageUtils;
import com.rzspider.common.utils.JsonUtils;
import com.rzspider.common.utils.StringUtils;
import com.rzspider.framework.config.FilePathConfig;
import com.rzspider.project.common.file.utilt.FileUtils;
import com.rzspider.project.commontool.toolmanage.domain.Toolmanage;
import com.rzspider.project.commontool.toolmanage.domain.Toolset;
import com.rzspider.project.commontool.toolmanage.service.IToolmanageService;
import com.rzspider.project.commontool.toolmanage.service.IToolsetService;
import com.rzspider.project.commontool.toolrun.domain.CobeImage;
import com.rzspider.project.commontool.toolrun.domain.CommonToolEntity;
import com.rzspider.project.commontool.toolrun.domain.FormatText;
import com.rzspider.project.commontool.toolrun.domain.GifMsg;
import com.rzspider.project.commontool.toolrun.domain.ImgToChar;
import com.rzspider.project.commontool.toolrun.domain.MatchRegularExpression;
import com.rzspider.project.commontool.toolrun.domain.ORCode;
import com.rzspider.project.commontool.toolrun.utils.BaiDuOCRReadUtil;
import com.rzspider.project.commontool.toolrun.utils.ToolrunUtils;

/**
 * ?????????????????? ?????????
 * 
 * @author ricozhou
 * @date 2018-07-23
 */
@Service
public class ToolrunServiceImpl implements IToolrunService {
	@Autowired
	private IToolmanageService toolmanageService;
	@Autowired
	private IToolsetService toolsetService;

	// ??????????????????
	@Override
	public int checkCommontoolStatus(Integer toolBackId) {
		// ??????????????????
		Toolmanage toolmanage = toolmanageService.selectToolmanageByToolBackId(toolBackId);
		if (toolmanage == null) {
			return 2;
		}
		if (toolmanage.getStatus() != 0) {
			return 1;
		}
		return 0;
	}

	// ???????????????
	@Override
	public boolean runORCodeCreate(ORCode orCode, String fileName) throws Exception {
		// ????????????
		if (orCode == null || CommonSymbolicConstant.EMPTY_STRING.equals(orCode.getContent())) {
			return false;
		}

		// ??????????????????
		Hashtable hints = new Hashtable();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		hints.put(EncodeHintType.CHARACTER_SET, CodingConstant.CODING_UTF_8);
		hints.put(EncodeHintType.MARGIN, 1);

		BitMatrix bitMatrix = new MultiFormatWriter().encode(orCode.getContent(), BarcodeFormat.QR_CODE,
				orCode.getOrCodeSizeW(), orCode.getOrCodeSizeH(), hints);
		int width = bitMatrix.getWidth();
		int height = bitMatrix.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
			}
		}
		// ??????logo
		if (orCode.isInsertLogo()) {
			// ???????????? ??????????????????
			image = ToolrunUtils.insertORLogoImage(image, orCode, true);
		}
		// ???????????????????????????????????????
		return FileUtils.imageToCachePath(image, orCode.getOrCodeImgFormat(), fileName);
	}

	// ???????????????
	@Override
	public String runORCodeAnalyze(CommonToolEntity commonToolEntity) {
		BufferedImage image;
		try {
			if (commonToolEntity.getImageUrl().startsWith(FileOtherConstant.FILE_JUMP_PATH_PREFIX2)) {
				// ?????????url
				String filePath = FilePathConfig.getUploadCachePath() + File.separator
						+ commonToolEntity.getImageUrl().substring(12);
				File file = new File(filePath);
				if (file.exists()) {
					image = ImageIO.read(file);
				} else {
					return null;
				}
			} else {
				URL url = new URL(commonToolEntity.getImageUrl());
				image = ImageIO.read(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// ?????????????????????????????????
			return null;
		}

		// ??????
		return ToolrunUtils.analyQR(image);
	}

	// ocr????????????
	@Override
	public String runOCRRead(CommonToolEntity commonToolEntity) {
		String imagePath;
		String exportContent = null;
		boolean isLocalImg;
		try {
			if (commonToolEntity.getImageUrl().startsWith(FileOtherConstant.FILE_JUMP_PATH_PREFIX2)) {
				isLocalImg = true;
				// ?????????url
				String filePath = FilePathConfig.getUploadCachePath() + File.separator
						+ commonToolEntity.getImageUrl().substring(12);
				File file = new File(filePath);
				if (file.exists()) {
					imagePath = file.getAbsolutePath();
				} else {
					return null;
				}
			} else {
				isLocalImg = false;
				imagePath = commonToolEntity.getImageUrl();
			}
			// ????????????????????????
			Toolset toolset = toolsetService.selectToolsetById(0);
			if (toolset.getToolOcrsetType() == 0) {
				exportContent = BaiDuOCRReadUtil.recognizeTextByBaiduOCR(toolset, imagePath, isLocalImg);
			} else if (toolset.getToolOcrsetType() == 1) {
				exportContent = BaiDuOCRReadUtil.recognizeTextByTesseractOCR(toolset, imagePath, isLocalImg);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// ?????????????????????????????????
			return null;
		}

		// ??????
		return exportContent;
	}

	// ???????????????
	@Override
	public String runFormatText(FormatText formatText) {
		if (formatText.getContent() == null) {
			return null;
		}
		if (formatText.getFormatFunction() == 0) {
			// json
			return JsonUtils.formatJson(formatText.getContent());
		} else if (formatText.getFormatFunction() == 1) {
			// ??????????????????
			return StringUtils.formatStringToOneString(formatText.getContent());
		}
		return null;
	}

	// ?????????????????????
	@Override
	public String[] runMatchRegularExpression(MatchRegularExpression matchRegularExpression) {
		if (matchRegularExpression == null || matchRegularExpression.getContent() == null
				|| matchRegularExpression.getRegularExpression() == null) {
			return null;
		}
		List<String> listString = StringUtils.GetRegResultToList(matchRegularExpression.getRegularExpression(),
				matchRegularExpression.getContent());
		StringBuilder sb = new StringBuilder();
		if (listString == null) {
			return null;
		}
		for (String str : listString) {
			sb.append(str);
			sb.append(CommonSymbolicConstant.LINEBREAK2);
		}
		return new String[] { String.valueOf(listString.size()), sb.toString() };
	}

	// ???????????????
	@Override
	public boolean runImgToChar(ImgToChar imgToChar, String fileName) {
		if (FileExtensionConstant.FILE_EXTENSION_IMAGE_GIF.equals(imgToChar.getImgPre().toLowerCase())) {
			// gif
			// ??????????????????
			List<BufferedImage> list = new ArrayList<BufferedImage>();
			// ?????????????????????
			List<BufferedImage> list2 = new ArrayList<BufferedImage>();
			// ??????????????????
			List<BufferedImage> list3 = new ArrayList<BufferedImage>();
			String filePath = imgToChar.getImageUrl();
			if (imgToChar.getImageUrl().startsWith(FileOtherConstant.FILE_JUMP_PATH_PREFIX2)) {
				filePath = FilePathConfig.getUploadCachePath() + File.separator + imgToChar.getImageUrl().substring(12);
			}
			// ??????gif
			GifMsg gifMsg = ImageUtils.splitGif(filePath);
			// ??????????????????????????????
			for (BufferedImage bi : gifMsg.getList()) {
				list.add(ImageUtils.zoom2(bi, imgToChar.getSfImgSizeW(), imgToChar.getSfImgSizeH()));
			}

			// ????????????????????????list??????????????????
			int w = list.get(0).getWidth();
			int h = list.get(0).getHeight();
			if (imgToChar.getCreateImgSizeW() > 0 && imgToChar.getCreateImgSizeH() > 0) {
				w = imgToChar.getCreateImgSizeW();
				h = imgToChar.getCreateImgSizeH();
			}
			for (int i = 0; i < list.size(); i++) {
				list2.add(ImageUtils.createBufferedImage(w, h));
			}
			// ?????????????????????
			int fontSize = 10;
			if (imgToChar.getCharSize() == 0) {
				// ?????????????????????
				// ????????????
				fontSize = 10;
			} else {
				fontSize = imgToChar.getCharSize();
			}
			// ?????????
			int imgIntensity = 3;
			if (imgToChar.getImgIntensity() == 0) {
				// ?????????????????????
				// ????????????
				imgIntensity = 3;
			} else {
				imgIntensity = imgToChar.getImgIntensity();
			}
			// ???????????????????????????????????????????????????????????????
			if (fontSize < 1 || imgIntensity < 1) {
				return false;
			}
			// ??????????????????????????????list3
			for (int i = 0; i < list2.size(); i++) {
				list3.add(ToolrunUtils.createAsciiPic(list.get(i), list2.get(i), imgToChar.getCharArray(), fontSize,
						imgIntensity));
			}
			// ???list????????????
			return ImageUtils.jpgToGif(list3, gifMsg.getList2(),
					FilePathConfig.getUploadCachePath() + File.separator + fileName);
		} else {
			// ????????????
			BufferedImage image = ImageUtils.getLocalAndUrlImage(imgToChar.getImageUrl());
			if (image == null) {
				return false;
			}
			// ??????????????????????????????
			image = ImageUtils.zoom2(image, imgToChar.getSfImgSizeW(), imgToChar.getSfImgSizeH());

			// ??????????????????????????????????????????
			int w = image.getWidth();
			int h = image.getHeight();
			if (imgToChar.getCreateImgSizeW() > 0 && imgToChar.getCreateImgSizeH() > 0) {
				w = imgToChar.getCreateImgSizeW();
				h = imgToChar.getCreateImgSizeH();
			}
			BufferedImage createImg = ImageUtils.createBufferedImage(w, h);
			// ?????????????????????
			int fontSize = 10;
			if (imgToChar.getCharSize() == 0) {
				// ?????????????????????
				// ????????????
				fontSize = 10;
			} else {
				fontSize = imgToChar.getCharSize();
			}
			int imgIntensity = 3;
			if (imgToChar.getImgIntensity() == 0) {
				// ?????????????????????
				// ????????????
				imgIntensity = 3;
			} else {
				imgIntensity = imgToChar.getImgIntensity();
			}
			// ???????????????????????????????????????????????????????????????
			if (fontSize < 1 || imgIntensity < 1) {
				return false;
			}
			// ??????
			createImg = ToolrunUtils.createAsciiPic(image, createImg, imgToChar.getCharArray(), fontSize, imgIntensity);
			// ??????
			if (createImg == null) {
				return false;
			}
			// ???????????????????????????????????????
			return FileUtils.imageToCachePath(createImg, FileUtils.getFileNameFromPoint(fileName), fileName);
		}

	}

	// ????????????
	@Override
	public boolean runCobeImageCreate(CobeImage cobeImage, String fileName) {
		// ??????????????????
		BufferedImage image = ImageUtils.getLocalAndUrlImage(cobeImage.getImageUrl());
		if (image == null) {
			return false;
		}
		// ???????????????????????????
		image = ImageUtils.zoom2(image, 1500, 1500);
		int cobeNum = 1;
		if (cobeImage.getCobeNum() > 1) {
			cobeNum = cobeImage.getCobeNum();
		}
		// ???????????????????????????????????????????????????????????????
		int[] sizeWH = ToolrunUtils.proCubeImag(image, cobeNum);

		// ??????,????????????????????????
		BufferedImage newImage = ToolrunUtils.createCobeImage(image, sizeWH);
		if (newImage == null) {
			return false;
		}
		// ????????????
		// ???????????????????????????????????????
		return FileUtils.imageToCachePath(newImage, FileUtils.getFileNameFromPoint(fileName), fileName);
	}

	// ???????????????
	@Override
	public String[] runFormatCode(FormatText formatText) {
		if (formatText.getContent() == null) {
			return null;
		}
		if (formatText.getFormatFunction() == 0) {
			// java
			String code = ToolrunUtils.formatJavaCode(formatText.getContent());
			if (CommonSymbolicConstant.EMPTY_STRING.equals(code)) {
				return new String[] { ReturnMessageConstant.RETURN_MESSAGE_FAILED, code };
			}
			return new String[] { ReturnMessageConstant.RETURN_MESSAGE_SUCCESS, code };
		} else if (formatText.getFormatFunction() == 1) {
			// python
			return null;
		} else if (formatText.getFormatFunction() == 2) {
			// js
			return null;
		} else if (formatText.getFormatFunction() == 3) {
			// html
			String code = ToolrunUtils.formatHtmlCode(formatText.getContent());
			if (code == null) {
				return new String[] { ReturnMessageConstant.RETURN_MESSAGE_FAILED, code };
			}
			return new String[] { ReturnMessageConstant.RETURN_MESSAGE_SUCCESS, code };
		} else if (formatText.getFormatFunction() == 4) {
			// sql
			String code = ToolrunUtils.formatSqlCode(formatText.getContent(), formatText.getSqlType());
			if (code == null) {
				return new String[] { ReturnMessageConstant.RETURN_MESSAGE_FAILED, code };
			}
			return new String[] { ReturnMessageConstant.RETURN_MESSAGE_SUCCESS, code };
		}
		return null;
	}
}
