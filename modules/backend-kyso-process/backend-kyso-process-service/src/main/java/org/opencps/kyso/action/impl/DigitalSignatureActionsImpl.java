package org.opencps.kyso.action.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.security.cert.Certificate;

import org.opencps.dossiermgt.model.DossierFile;
import org.opencps.dossiermgt.model.DossierPart;
import org.opencps.dossiermgt.service.DossierFileLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierPartLocalServiceUtil;
import org.opencps.kyso.action.DigitalSignatureActions;
import org.opencps.kyso.utils.BCYSignatureUtil;
import org.opencps.kyso.utils.CertUtil;
import org.opencps.kyso.utils.ExtractTextLocations;
import org.opencps.kyso.utils.ImageUtil;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

import backend.kyso.process.service.util.ConfigProps;
import vgca.svrsigner.ServerSigner;

public class DigitalSignatureActionsImpl implements DigitalSignatureActions{

	private static Log _log = LogFactoryUtil.getLog(DigitalSignatureActionsImpl.class);

	@Override
	public JSONObject createHashSignature(User user, long id, String[] idSplit) {
			byte[] inHash = null;
			String fieldName = StringPool.BLANK;
			String fullPathSigned = StringPool.BLANK;
			JSONObject jsonFeed = JSONFactoryUtil.createJSONObject();
			JSONArray hashComputers = JSONFactoryUtil.getJSONFactory()
					.createJSONArray();
			JSONArray signFieldNames = JSONFactoryUtil.getJSONFactory()
					.createJSONArray();
			JSONArray fileNames = JSONFactoryUtil.getJSONFactory()
					.createJSONArray();
			JSONArray messages = JSONFactoryUtil.getJSONFactory().createJSONArray();
			JSONArray fullPathOfSignedFiles = JSONFactoryUtil.getJSONFactory()
					.createJSONArray();

			String realPath = PropsUtil.get(ConfigProps.CER_HOME)+"/";
			_log.info("realPath: "+realPath);
			//==
//			ActionRequest request = null;
//			String realPath = ReportUtils.getTemplateReportFilePath(request);
			String fullPath = StringPool.BLANK;

			try {
				String emailUser = user.getEmailAddress();
				float offsetX = 1;
				float imageZoom = 1;
				float offsetY = 1;
				for (String strId : idSplit) {
					String[] idArr = strId.split(StringPool.COMMA);
					DossierPart dossierPart = DossierPartLocalServiceUtil.fetchDossierPart(Long.valueOf(idArr[1]));
					_log.info("Dossier Part: "+dossierPart);
					DossierFile dossierFile = null;
					if (dossierPart != null && dossierPart.getESign()) {
						dossierFile = DossierFileLocalServiceUtil.fetchDossierFile(Long.valueOf(idArr[0]));
						_log.info("Dossier File: "+dossierFile);
						if (dossierFile != null && dossierFile.getFileEntryId() > 0) {
							long fileEntryId = dossierFile.getFileEntryId();
							_log.info("fileEntryId: "+fileEntryId);
//							long fileEntryId = 0;

							DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.fetchDLFileEntry(fileEntryId);
							
							File fileTemp = FileUtil.createTempFile(dlFileEntry.getContentStream());
							
							File file = new File(realPath + dlFileEntry.getFileName());
							
							FileUtil.move(fileTemp, file);
							
							fullPath = file.getAbsolutePath();
							_log.info("fullPath: "+fullPath);

							String signImagePath = new File(realPath + emailUser + ".png").getAbsolutePath();
							String imageBase64 = ImageUtil.getSignatureImageBase64ByPath(signImagePath);
							_log.info("signImagePath: "+signImagePath);
							_log.info("imageBase64: "+imageBase64);

							BufferedImage bufferedImage = ImageUtil.getImageByPath(signImagePath);

							Certificate cert = CertUtil.getCertificateByPath(new File(realPath + emailUser + ".cer").getAbsolutePath());

							boolean showSignatureInfo = true;

							ServerSigner signer = BCYSignatureUtil.getServerSigner(fullPath, cert, imageBase64, showSignatureInfo);

							signer.setSignatureGraphic(imageBase64);
							signer.setSignatureAppearance(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION);

							ExtractTextLocations textLocation = new ExtractTextLocations(fullPath);

							_log.info("*********************************" + textLocation.getAnchorX() + "-"
									+ textLocation.getAnchorY() + "********************************");

							_log.info("*********************************" + textLocation.getPageLLX() + "-"
									+ textLocation.getPageURX() + "-" + textLocation.getPageLLY() + "-" + textLocation.getPageURY()
									+ "*******************************");

							// tinh kich thuoc cua anh

							int signatureImageWidth = (bufferedImage != null && bufferedImage.getWidth() > 0)
									? bufferedImage.getWidth() : 80;

							int signatureImageHeight = (bufferedImage != null && bufferedImage.getHeight() > 0)
									? bufferedImage.getHeight() : 80;
							
							float llx = textLocation.getAnchorX() + offsetX;

							float lly = textLocation.getAnchorY() - signatureImageHeight * imageZoom + offsetY;

							if (textLocation.getAnchorX() > 200) {
								llx = llx - 100;
							}
							if (textLocation.getAnchorY() > 420) {
								lly = lly - 420;
							}
			
							if (lly < 0) {
								lly = 0;
							}
							if (llx < 0) {
								llx = 0;
							}
							float urx = llx + signatureImageWidth * imageZoom;
							float ury = lly + signatureImageHeight * imageZoom;
							// tinh kich thuoc cua anh
//							int signatureImageWidth = (bufferedImage != null && bufferedImage
//									.getWidth() > 0) ? bufferedImage.getWidth() : 80;
//							int signatureImageHeight = (bufferedImage != null && bufferedImage
//									.getHeight() > 0) ? bufferedImage
//									.getHeight() : 80;
//							float llx = textLocation.getAnchorX();
//							float urx = llx + signatureImageWidth / 3;
		
//							float lly = textLocation.getPageURY()
//									- textLocation.getAnchorY()
//									- signatureImageHeight / 3;

//							float ury = lly + signatureImageHeight / 3;

							inHash = signer.computeHash(new Rectangle(llx, lly , urx, ury), 1);
//							inHash = signer.computeHash(
//									new Rectangle(llx + 20, lly - 105,
//											urx + 94, ury - 70), 1);
							_log.info("********************************* llx " + llx);

							_log.info("********************************* lly " + lly);
							
							_log.info("********************************* urx " + urx);
							
							_log.info("********************************* ury " + ury);
							
							_log.info("********************************* signatureImageWidth " + signatureImageWidth);
							
							_log.info("********************************* signatureImageHeight " + signatureImageHeight);

//							signature = Base64.getDecoder().decode("");
//							signer.completeSign(signature, fieldName);
//							File fileSigned = new File(fullPath.replace(".pdf", ".signed.pdf"));
//							fieldName = signer.getSignatureName();
//							fileNames.put(urlFile);

							fieldName = signer.getSignatureName();
							fullPathSigned = signer.getSignedFile();
							hashComputers.put(Base64.encode(inHash));
							signFieldNames.put(fieldName);
							fileNames.put(dlFileEntry.getFileName());
							messages.put("success");
							fullPathOfSignedFiles.put(fullPathSigned);
							
							_log.info("hashComputers: "+hashComputers);
							_log.info("signFieldNames: "+signFieldNames);
							_log.info("fullPathOfSignedFiles: "+fullPathOfSignedFiles);
							_log.info("fileNames: "+fileNames);
							_log.info("messages: "+messages);
							

							_log.info("===KY XONG YCGIAMDIDNH===: "+ fullPathSigned);
							

//							ServiceContext serviceContext = new ServiceContext();

//							DLAppLocalServiceUtil.updateFileEntry(user.getUserId(), dlFileEntry.getFileEntryId(), dlFileEntry.getTitle(),
//									dlFileEntry.getMimeType(), dlFileEntry.getTitle(), dlFileEntry.getDescription(),
//									StringPool.BLANK, false, fileSigned, serviceContext);
							
							// turnOn DossierFile Sync
							
//							JSONObject msgDataIn = JSONFactoryUtil.createJSONObject();
//							msgDataIn.put("dossierFileId", dossierFileId);
//							msgDataIn.put("dossierFileSync", eSign);
//							msgDataIn.put("userId", userId);
							
//							message.put("msgToEngine", msgDataIn);
//							MessageBusUtil.sendMessage("jasper/dossier/in/destination", message);
							break;
						}
					}
				}
			} catch (Exception e) {
				hashComputers.put(StringPool.BLANK);
				signFieldNames.put(StringPool.BLANK);
				fileNames.put(StringPool.BLANK);
				fullPathOfSignedFiles.put(StringPool.BLANK);
				messages.put(e.getClass().getName());
				
				_log.error(e);
			}
			
			jsonFeed.put("hashComputers", hashComputers);
			jsonFeed.put("signFieldNames", signFieldNames);
			jsonFeed.put("fileNames", fileNames);
			jsonFeed.put("msg", messages);
			jsonFeed.put("fullPathSigned", fullPathOfSignedFiles);
			_log.info("=====CHECK END kyDuyetYCGiamDinh=====" + jsonFeed.toString());

			return jsonFeed;

		
	}

	@Override
	public JSONObject completeSignature(User user, long id, String sign, String signFieldName, String fileName) {

//		String sign = ParamUtil.getString(request, "sign");
//		String signFieldName = ParamUtil.getString(request, "signFieldName");
//		String fileName = ParamUtil.getString(request, "fileName");
		
		JSONObject jsonFeed = JSONFactoryUtil.createJSONObject();
		
		if (Validator.isNotNull(sign) && Validator.isNotNull(fileName)) {
			byte[] signature = Base64.decode(sign);

			String realPath = PropsUtil.get(ConfigProps.CER_HOME)+"/";
//			String realPath = ReportUtils.getTemplateReportFilePath(request);
//			String fullPath = StringPool.BLANK;

//			String realExportPath = realPath + "/export/";
			try {
				fileName = fileName.substring(0,
						fileName.lastIndexOf(StringPool.PERIOD));
				
				ServerSigner signer = new ServerSigner(realPath
						+ fileName + ".pdf", null);

				signer.completeSign(signature, signFieldName);

//				String signedFile = signer.getSignedFile();

//				fullPath = realPath + fileName + ".pdf";
				
//				File fileSigned = new File(fullPath.replace(".pdf", ".signed.pdf"));

//				ServiceContext serviceContext = new ServiceContext();

				
//				DLAppLocalServiceUtil.updateFileEntry(user.getUserId(), dlFileEntry.getFileEntryId(), dlFileEntry.getTitle(),
//						dlFileEntry.getMimeType(), dlFileEntry.getTitle(), dlFileEntry.getDescription(),
//						StringPool.BLANK, false, fileSigned, serviceContext);

				jsonFeed.put("msg", "success");
//				log.info("===signatureCompleteKyDuyetYCGiamDinh===success");
			} catch (Exception e) {
				_log.error(e);
				jsonFeed.put("msg", "error");
			}
		} else {
			jsonFeed.put("msg", "error");
			_log.info("Cannot sign the file: " + fileName);
		}
		
		return jsonFeed;
	}

}
