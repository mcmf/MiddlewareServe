/**
 * 
 */
package com.its.core.module.device.vehicle;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.its.core.common.DeviceInfoBean;
import com.its.core.common.DeviceInfoLoaderFactory;
import com.its.core.module.device.ACommunicateProcessor;
import com.its.core.module.device.MessageBean;
import com.its.core.module.device.MessageHelper;
import com.its.core.module.device.vehicle.filter.IProcessorFilter;
import com.its.core.util.DateHelper;
import com.its.core.util.XMLParser;
import com.its.core.util.XMLProperties;

/**
 * �������� 2013-6-26 ����03:52:33
 * @author GuoPing.Wu
 * Copyright: Xinoman Technologies CO.,LTD.
 */
public class RealtimeVehicleAloneProcessor extends ACommunicateProcessor {
	private static final Log log = LogFactory.getLog(RealtimeVehicleAloneProcessor.class);
	
	private List<IProcessorFilter> filterList = new ArrayList<IProcessorFilter>();

	/* (non-Javadoc)
	 * @see com.its.core.module.device.ACommunicateProcessor#configure(com.its.core.util.XMLProperties, java.lang.String, int)
	 */
	@Override
	public void configure(XMLProperties props, String propertiesPrefix, int no) throws Exception {		
		
		String filterPrefix = props.getProperty(propertiesPrefix,no,"filter");
		int size = props.getPropertyNum(filterPrefix);
		log.debug("filter size = "+size);
		for(int i=0;i<size;i++){
			String valid = props.getProperty(filterPrefix,i,"valid");
			if("true".equalsIgnoreCase(valid) || "y".equalsIgnoreCase(valid)){
				String filterClass = props.getProperty(filterPrefix,i,"class");
				if(filterClass!=null && !filterClass.trim().equals("")){
					try{				
						IProcessorFilter filter = (IProcessorFilter)Class.forName(filterClass).newInstance();
						filter.configure(props,filterPrefix,i);	
						filterList.add(filter);
						log.debug("Loaded : "+filterClass);
					}catch(Exception ex){
						log.error("��ʼ��ʵʱ����FILTER��["+filterClass+"]ʱ������"+ex.getMessage(),ex);
					}
				}					
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.its.core.module.device.ACommunicateProcessor#process(com.its.core.module.device.MessageBean)
	 */
	@Override
	public void process(MessageBean messageBean) throws Exception {
		RealtimeVehicleInfoBean realtimeVehicleInfoBean = this.parseMessage(messageBean);
		
		//��FILTER������Ҫ���δ���ʵʱ������Ϣ
		int filterSize = this.filterList.size();
		for(int i=0;i<filterSize;i++){
			IProcessorFilter filter = this.filterList.get(i);
			if(!filter.process(realtimeVehicleInfoBean)){
				break;
			}
		}

	}	
	
	/**
	 * ����message
	 * @param messageBean
	 * @return
	 * @throws Exception
	 */
	protected RealtimeVehicleInfoBean parseMessage(MessageBean messageBean) throws Exception{
		RealtimeVehicleInfoBean realtimeVehicleInfoBean = new RealtimeVehicleInfoBean();		
		realtimeVehicleInfoBean.setProtocolBean(messageBean);

		/* ��ϢЭ���ʽʾ����
			<?xml version="1.0" encoding="UTF-8"?>
			<message>
				<head>
					<code>DS1005</code>	
					<version>2.0.0.0</version>
				</head>
				<body>
					<content>
						<deviceId></deviceId>
						<directionCode></directionCode>
						<directionNo></directionNo>
						<plateNo></plateNo>
						<plateColor></plateColor>
						<catchTime></catchTime>
						<laneNo></laneNo>
						<speed></speed>
						<limitSpeed></limitSpeed>
						<featureImagePath></featureImagePath>
						<panoramaImagePath></panoramaImagePath>
					</content>
				</body>
			</message>			
		*/
		XMLParser xmlParser = messageBean.getXmlParser();
		realtimeVehicleInfoBean.setDeviceId(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "sbbh"));
//		realtimeVehicleInfoBean.setDirectionCode(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "fxbh"));
		realtimeVehicleInfoBean.setPlateNo(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "hphm"));
		realtimeVehicleInfoBean.setPlateColor(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "hpys"));
		
		String catchTime = xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "jgsj");
		if(catchTime.length()>14){
			catchTime = catchTime.substring(0,14);
		}
		realtimeVehicleInfoBean.setCatchTime(DateHelper.parseDateString(catchTime,"yyyyMMddHHmmss"));
		realtimeVehicleInfoBean.setDrivewayNo(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "cdbh"));
		realtimeVehicleInfoBean.setSpeed(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "clsd"));
		realtimeVehicleInfoBean.setLimitSpeed(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "clxs"));
		realtimeVehicleInfoBean.setFeatureImagePath(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "txtp"));
		realtimeVehicleInfoBean.setPanoramaImagePath(xmlParser.getProperty(MessageHelper.XML_ELE_CONTENT_PREFIX + "qjtp"));	

		//Ĭ��ʹ��ϵͳ�豸��Ϣ����
		try {
			DeviceInfoBean dib = (DeviceInfoBean)DeviceInfoLoaderFactory.getInstance().getDeviceMap().get(realtimeVehicleInfoBean.getDeviceId());
//			realtimeVehicleInfoBean.setDirectionDrive(dib.getDirectionDrive());	
			realtimeVehicleInfoBean.setDirectionCode(dib.getDirectionCode());
		}catch(Exception ex){}	
		
		return realtimeVehicleInfoBean;
	}

}