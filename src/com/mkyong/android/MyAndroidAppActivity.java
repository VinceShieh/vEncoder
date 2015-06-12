package com.mkyong.android;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MyAndroidAppActivity extends Activity {

	private Spinner spinner1, spinner2, spinner_color;
	private Button btnSubmit;
	private Button btnCap;
	private static final int TEST_Y = 120;                  // YUV values for colored rect
    private static final int TEST_U = 160;
    private static final int TEST_V = 200;
	private static int mWidth ;//1280;
	private static int mHeight;//720;
	private int num_frame;
	private int timeElapsed;
    private int mBitRate = 6000000;
    private static final String TAG = "EncoderTest";
    private static final boolean VERBOSE = true;           // lots of logging
    private static final boolean DEBUG_SAVE_FILE = true;   // save copy of encoded movie
    private static final boolean READ_FILE = true;
    private static final String DEBUG_FILE_NAME_BASE = "/sdcard/Movies/output.";
    private static  String mfilename="";
    private static final int REQUESTCODE_PICK_VIDEO = 11;
    // parameters for the encoder
    private static String MIME_TYPE = "";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int QUEUE_INPUT_BUFFER =111;
    // movie length, in frames
    private static final int NUM_FRAMES = 1000;               // two seconds of video
    private static final int MAX_FRAME_NUMBER =100;
	public static final int FRAME_RATE =30;
	ProgressDialog progressBar;
	private Handler progressBarHandler = new Handler();
	private int progressBarStatus = 0;
	 private int progressStatus = 0;
	 private TextView textView;
//	 private Handler handler = new Handler();
	 private FileInputStream inFile = null;
	 private CircularEncoder mCircEncoder;

	 public int frameCount=0;
     public static int jumpTime = 0;
    public static final int DEFAULT_BITRATE=10*1000000;  //10Mbps
    public static final int MAX_ENCODE_FRAMES=1000;
    byte[] input_data = null;//new byte[MAX_FRAME_NUMBER * mWidth * mHeight * 3 / 2];
    byte[] input_one_buf=null;
    private ProgressBar circleProgressBar;
    private ProgressBar rectProgressBar;
    public int frame_num=0;
    private int icount=0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		circleProgressBar = (ProgressBar)findViewById(R.id.circleProgressBar);
		circleProgressBar.setIndeterminate(false);
		rectProgressBar=(ProgressBar)findViewById(R.id.mprogressBar);
		rectProgressBar.setIndeterminate(false);
		addItemsOnSpinner();
		addListenerOnSpinnerItemSelection();
		addListenerOnButton();
		
		
	
 }

	public void addItemsOnSpinner() {
		spinner1 = (Spinner) findViewById(R.id.spinner1);
		List<String> list = new ArrayList<String>();
		spinner_color = (Spinner) findViewById(R.id.spin_color);
		List<String> list_color = new ArrayList<String>();
		int numCodecs = MediaCodecList.getCodecCount();
        
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
            	if(types[j].startsWith("video")){
            		if(list.contains(types[j]))Log.d(TAG, "supported codec: " +types[j]+" already exists");
            		else{
		                Log.d(TAG, "****supported codec: " +types[j]);
		                list.add(types[j]);
	                }
	            }
            }
          /*  
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
            for (int k = 0; k < capabilities.colorFormats.length; k++) {
                int colorFormat = capabilities.colorFormats[k];
                switch (colorFormat) {
                // these are the formats we know how to handle for this test
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                	
                	if(list_color.contains("COLOR_FormatYUV420Planar"))Log.d(TAG, "COLOR_FormatYUV420Planar exists");
                	else{
                		Log.d(TAG, "add COLOR_FormatYUV420Planar");
                		list_color.add("COLOR_FormatYUV420Planar");
                	}
                	break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                	if(list_color.contains("COLOR_FormatYUV420PackedPlanar"))Log.d(TAG, "COLOR_FormatYUV420PackedPlanar exists");
                	else{
                		Log.d(TAG, "add COLOR_FormatYUV420PackedPlanar");
                		list_color.add("COLOR_FormatYUV420PackedPlanar");
                	}
                	break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                	if(list_color.contains("COLOR_FormatYUV420SemiPlanar"))Log.d(TAG, "COLOR_FormatYUV420SemiPlanar exists");
                	else{
                		Log.d(TAG, "add COLOR_FormatYUV420SemiPlanar");
                		list_color.add("COLOR_FormatYUV420SemiPlanar");
                	}
                	break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                	if(list_color.contains("COLOR_FormatYUV420PackedSemiPlanar"))Log.d(TAG, "COLOR_FormatYUV420PackedSemiPlanar exists");
                	else{
                		Log.d(TAG, "add COLOR_FormatYUV420PackedSemiPlanar");
                		list_color.add("COLOR_FormatYUV420PackedSemiPlanar");
                	}
                	break;
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                	if(list_color.contains("COLOR_TI_FormatYUV420PackedSemiPlanar"))Log.d(TAG, "COLOR_TI_FormatYUV420PackedSemiPlanar exists");
                	else{
                		Log.d(TAG, "add COLOR_TI_FormatYUV420PackedSemiPlanar");
                		list_color.add("COLOR_TI_FormatYUV420PackedSemiPlanar");
                	}
                	break;
                    
                default:
                	Log.d(TAG, "****no supported color format found");    
            }
                Log.d(TAG, "****color format: "+colorFormat);
            }
        */
        }
		
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner1.setAdapter(dataAdapter);
		
		ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list_color);
		colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_color.setAdapter(colorAdapter);
	}

	//add items into spinner dynamically
	public void addItemsOnSpinner2() {

		spinner2 = (Spinner) findViewById(R.id.spinner2);
		List<String> list = new ArrayList<String>();
  
		list.add("list 1");
		list.add("list 2");
		list.add("list 3");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(dataAdapter);
	}

	public void addListenerOnSpinnerItemSelection(){
		
	//	spinner1 = (Spinner) findViewById(R.id.spinner1);
	//	spinner1.setOnItemSelectedListener(new CustomOnItemSelectedListener());
	//	spinner2 = (Spinner) findViewById(R.id.spinner2);
	//	spinner2.setOnItemSelectedListener(new CustomOnItemSelectedListener());		
	//	spinner_color = (Spinner) findViewById(R.id.spin_color);
	//	spinner_color.setOnItemSelectedListener(new CustomOnItemSelectedListener());
		Spinner spinner_dump=(Spinner) findViewById(R.id.spinner_dump);
		spinner_dump.setOnItemSelectedListener(new CustomOnItemSelectedListener());
	}
	


      private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }  
      
      /**
       * Returns the first codec capable of encoding the specified MIME type, or null if no
       * match was found.
       */
      private static MediaCodecInfo selectCodec(String mimeType) {
          int numCodecs = MediaCodecList.getCodecCount();
          
          for (int i = 0; i < numCodecs; i++) {
              MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

              if (!codecInfo.isEncoder()) {
                  continue;
              }

              String[] types = codecInfo.getSupportedTypes();
              for (int j = 0; j < types.length; j++) {
                  Log.d(TAG, "****supported codec: " +types[j]);
              }
              for (int j = 0; j < types.length; j++) {
                  if (types[j].equalsIgnoreCase(mimeType)) {
                      return codecInfo;
                  }
              }
          }
          return null;
      }
      /**
       * Returns a color format that is supported by the codec and by this test code.  If no
       * match is found, this throws a test failure -- the set of formats known to the test
       * should be expanded for new platforms.
       */
      private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
          MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
          for (int i = 0; i < capabilities.colorFormats.length; i++) {
              int colorFormat = capabilities.colorFormats[i];
              if (isRecognizedFormat(colorFormat)) {
                  return colorFormat;
              }
              Log.d(TAG, "****color format: "+colorFormat);
          }
//          fail("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
          Log.e(TAG,"couldn't find a good color format" );
          return 0;   // not reached
      }
      /**
       * Returns true if this is a color format that this test code understands (i.e. we know how
       * to read and generate frames in this format).
       */
      private static boolean isRecognizedFormat(int colorFormat) {
          switch (colorFormat) {
              // these are the formats we know how to handle for this test
              case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
              case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
              case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
              case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
              case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                  return true;
              default:
                  return false;
          }
      }
/**
 * Returns true if the specified color format is semi-planar YUV.  Throws an exception
 * if the color format is not recognized (e.g. not YUV).
 */
private static boolean isSemiPlanarYUV(int colorFormat) {
    switch (colorFormat) {
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            return false;
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
        case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
        case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
            return true;
        default:
            throw new RuntimeException("unknown format " + colorFormat);
    }
}

private void read_all(FileInputStream inFile,byte[] YUV,int size){
	//read whole input yuv 
    int frame_count = 0;
    boolean eof=false;
    int k=0;
    try{
		inFile.read(YUV, 0, size);
		//frame_count=3;
		
    
  if (false) {
	//  	while(!eof){
	for (int j = 0; j < MAX_FRAME_NUMBER; j++) {
		for (int i = 0; i < size; i++) {
			YUV[k] = (byte) inFile.read();

			if (-1 == YUV[k]) {
				eof = true;
				Log.d(TAG, "*******read_all completed, read " + frame_count
						+ " frames in total");
				//return frame_count;
			}
			k++;
		}

		frame_count++;
		//Log.d(TAG, "***read " + frame_count + "frame");
	}
	//	}
}
    }catch(FileNotFoundException fe) {
        System.out.println("There is no such file!");

    } catch(IOException ioe) {
        System.out.println("IO exception!");

    }
 //   return frame_count;
  }
private int read_t(FileInputStream inFile,byte[] YUV,int size){
	//read one frame
    int ret = 0;
    try{
    for(int i=0;i<size;i++)
    {
        YUV[i]=(byte) inFile.read() ;
        if(-1==YUV[i])
        {
        	return -1;
        }
    }
 //   return ret;
    }catch(FileNotFoundException fe) {
        System.out.println("There is no such file!");

    } catch(IOException ioe) {
        System.out.println("IO exception!");

    }
    return 0;
  }


private void read_one(FileInputStream inFile,byte[] YUV,int size){
	//read whole input yuv 
    int frame_count = 0;
    boolean eof=false;
    int k=0;
    try{
		inFile.read(YUV, 0, size);
    	 

    }catch(FileNotFoundException fe) {
        System.out.println("There is no such file!");

    } catch(IOException ioe) {
        System.out.println("IO exception!");

    }
  }

/**
 * Generates data for frame N into the supplied buffer.  We have an 8-frame animation
 * sequence that wraps around.  It looks like this:
 * <pre>
 *   0 1 2 3
 *   7 6 5 4
 * </pre>
 * We draw one of the eight rectangles and leave the rest set to the zero-fill color.
 */
private void generateFrame(int frameIndex, int colorFormat, byte[] frameData) {
    final int HALF_WIDTH = mWidth / 2;
    boolean semiPlanar = isSemiPlanarYUV(colorFormat);

    // Set to zero.  In YUV this is a dull green.
    Arrays.fill(frameData, (byte) 0);

    int startX, startY, countX, countY;

    frameIndex %= 8;
    //frameIndex = (frameIndex / 8) % 8;    // use this instead for debug -- easier to see
    if (frameIndex < 4) {
        startX = frameIndex * (mWidth / 4);
        startY = 0;
    } else {
        startX = (7 - frameIndex) * (mWidth / 4);
        startY = mHeight / 2;
    }

    for (int y = startY + (mHeight/2) - 1; y >= startY; --y) {
        for (int x = startX + (mWidth/4) - 1; x >= startX; --x) {
            if (semiPlanar) {
                // full-size Y, followed by UV pairs at half resolution
                // e.g. Nexus 4 OMX.qcom.video.encoder.avc COLOR_FormatYUV420SemiPlanar
                // e.g. Galaxy Nexus OMX.TI.DUCATI1.VIDEO.H264E
                //        OMX_TI_COLOR_FormatYUV420PackedSemiPlanar
                frameData[y * mWidth + x] = (byte) TEST_Y;
                if ((x & 0x01) == 0 && (y & 0x01) == 0) {
                    frameData[mWidth*mHeight + y * HALF_WIDTH + x] = (byte) TEST_U;
                    frameData[mWidth*mHeight + y * HALF_WIDTH + x + 1] = (byte) TEST_V;
                }
            } else {
                // full-size Y, followed by quarter-size U and quarter-size V
                // e.g. Nexus 10 OMX.Exynos.AVC.Encoder COLOR_FormatYUV420Planar
                // e.g. Nexus 7 OMX.Nvidia.h264.encoder COLOR_FormatYUV420Planar
                frameData[y * mWidth + x] = (byte) TEST_Y;
                if ((x & 0x01) == 0 && (y & 0x01) == 0) {
                    frameData[mWidth*mHeight + (y/2) * HALF_WIDTH + (x/2)] = (byte) TEST_U;
                    frameData[mWidth*mHeight + HALF_WIDTH * (mHeight / 2) +
                              (y/2) * HALF_WIDTH + (x/2)] = (byte) TEST_V;
                }
            }
        }
    }
}

      //get the selected dropdown list value
	public void addListenerOnButton() {

		spinner1 = (Spinner) findViewById(R.id.spinner1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		spinner_color=(Spinner) findViewById(R.id.spin_color);
/*
		btnCap=(Button) findViewById(R.id.btnCap);
		btnCap.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int fps=0;
				if(spinner2.getSelectedItemPosition()==0)fps=30;
				else if(spinner2.getSelectedItemPosition()==1)fps=60;
		
				MediaCodecInfo codecInfo = selectCodec("video/avc");
	            if (codecInfo == null) {
	                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
	                Log.e(TAG, "vin:Unable to find an appropriate codec for " + MIME_TYPE);
	                return;
	            }
	            if (VERBOSE) Log.d(TAG, "vin:found codec: " + codecInfo.getName());

	            int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
	            if (VERBOSE) Log.d(TAG, "vin:found colorFormat: " + colorFormat);
	            
	           try {
	            	
	            	TextView color=(TextView)findViewById(R.id.tv_color);
	            	 switch (colorFormat) {
	                 case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
	                	 color.setText("COLOR_FormatYUV420Planar");
	                	 break;
	                 case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
	                	 color.setText("COLOR_FormatYUV420PackedPlanar");
	                	 break;
	                 case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
	                	 color.setText("COLOR_FormatYUV420SemiPlanar");
	                	 break;
	                 case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
	                	 color.setText("COLOR_FormatYUV420PackedSemiPlanar");
	                	 break;
	                 case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
	                	 color.setText("COLOR_TI_FormatYUV420PackedSemiPlanar");
	                	 break;
	                 default:
	                     throw new RuntimeException("unknown format " + colorFormat);
	             }
	            	 EditText et_num = (EditText)findViewById(R.id.frame_num);
					int frame_num=Integer.parseInt(et_num.getText().toString());
	            	Log.d(TAG, "vin:Creating Encoder****"+"width:"+mWidth+",height:"+mHeight+",color:"+colorFormat
	            			+",fps:"+fps+",frame number:"+frame_num);
					
	            	
	                mCircEncoder = new CircularEncoder(mWidth, mHeight, 6000000,
	                        30, colorFormat, input_one_buf);
	                
				    new Thread(new on_encode_thread(frame_num,fps)).start();
				    

	              
	            } catch (IOException ioe) {
	                throw new RuntimeException(ioe);
	            }	
	            
			}

		});
		*/
		btnSubmit = (Button) findViewById(R.id.btnSubmit);
		
		btnSubmit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText et_w = (EditText)findViewById(R.id.et_w);
				EditText et_h = (EditText)findViewById(R.id.et_h);
				EditText et_num = (EditText)findViewById(R.id.frame_num);
				EditText et_time = (EditText)findViewById(R.id.et_time);
				EditText et_framerate = (EditText)findViewById(R.id.et_framerate);
				EditText et_bitrate = (EditText)findViewById(R.id.et_bitrate);
				EditText et_gop = (EditText)findViewById(R.id.et_gop);
				//if(et_w.length()==0||et_w.equals("")||et_h.length()==0||et_h.equals("")||input_one_buf==null){
				if(et_w.length()==0||et_w.equals("")||et_h.length()==0||et_h.equals("")||input_data==null){
					Toast.makeText(MyAndroidAppActivity.this, " Input file empty! Please input width, height then choose a file ",15000).show();
				}else{
					
					String mime=spinner1.getSelectedItem().toString();
					Log.d(TAG, "getSelectItem codec is: "+mime);
				//	String color=spinner_color.getSelectedItem().toString();
				//	Log.d(TAG, "getSelectItem color is: "+color);
					MediaCodecInfo info=selectCodec(mime);
					int colorFormat=selectColorFormat(info, mime);
					Log.d(TAG, "selected Codec: "+mime+",color format: "+colorFormat );
					MIME_TYPE=mime;
					
					int fps=0;
					if(spinner2.getSelectedItemPosition()==0)fps=30;
					else if(spinner2.getSelectedItemPosition()==1)fps=60;
					else if(spinner2.getSelectedItemPosition()==2)fps=120;
			
					MediaCodecInfo codecInfo = selectCodec(mime);
		            if (codecInfo == null) {
		                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
		                Log.e(TAG, "vin:Unable to find an appropriate codec for " + MIME_TYPE);
		                return;
		            }
		            if (VERBOSE) Log.d(TAG, "vin:found codec: " + codecInfo.getName());
	
		            try {
		            	mWidth=Integer.parseInt(et_w.getText().toString());
						mHeight=Integer.parseInt(et_h.getText().toString());
						num_frame=Integer.parseInt(et_num.getText().toString());
						timeElapsed=Integer.parseInt(et_time.getText().toString());
		       //     	 EditText et_num = (EditText)findViewById(R.id.frame_num);
				//		int num_frame=Integer.parseInt(et_num.getText().toString());
						frame_num=num_frame;
		  
						int frame_total=fps*timeElapsed;

		    			int mframerate, mbitrate,iInterval;
						if(et_framerate.length()==0||et_framerate.equals("")){
							mframerate=30;
							Log.d(TAG, "vin:set framerate default:30fps");
						}else{
							mframerate=Integer.parseInt(et_framerate.getText().toString());
						}
						if(et_bitrate.length()==0||et_bitrate.equals("")){
							mbitrate=2000000;//2Mbps
							Log.d(TAG, "vin:set bitrate default:2Mbps");
						}else{
							mbitrate=Integer.parseInt(et_bitrate.getText().toString());
						}
						if(et_gop.length()==0||et_gop.equals("")){
							iInterval=30;
							Log.d(TAG, "vin:set I-frame interval default:30");
						}else{
							iInterval=Integer.parseInt(et_gop.getText().toString());
						}
			          	Log.d(TAG, "vin:Creating Encoder****type:"+mime+",width:"+mWidth+",height:"+mHeight+",color:"+colorFormat
		            			+",fps:"+fps+",frame number:"+frame_num+",framerate:"+mframerate+",bitrate:"+mbitrate+",I-frame interval:"+iInterval);
		      //          mCircEncoder = new CircularEncoder(mWidth, mHeight, DEFAULT_BITRATE,FRAME_RATE, colorFormat, input_one_buf);
			          	boolean dump=false;
			    		Spinner spinner_dump=(Spinner) findViewById(R.id.spinner_dump);

			          	if(spinner_dump.getSelectedItemPosition()==0)dump=false;
						else if(spinner_dump.getSelectedItemPosition()==1){
							dump=true;
							Toast.makeText(MyAndroidAppActivity.this, "Encoding output will be written to /sdcard/Movies/output_xx.h264", 5000);
						}
						
						mCircEncoder = new CircularEncoder(mime,mWidth, mHeight, mbitrate,mframerate,iInterval, colorFormat, input_data,frame_num, dump);
					   // new Thread(new on_encode_thread(frame_num,fps)).start();
					    new Thread(new on_encode_thread(frame_total,fps,frame_num)).start();

					    
					    rectProgressBar.setVisibility(View.VISIBLE);
					    rectProgressBar.setMax(frame_total);
					    rectProgressBar.setProgress(0);
		            } catch (IOException ioe) {
		                throw new RuntimeException(ioe);
		            }	
				//	encode2(mWidth, mHeight,FRAME_RATE , true, MIME_TYPE, colorFormat);
				//	Toast.makeText(MyAndroidAppActivity.this, "width:" + mWidth + "height" + mHeight, 5000).show();
				//	Toast.makeText(MyAndroidAppActivity.this, "Encoding Complete", 5000).show();
				//	progressBar.setProgress(100);
					

			  }	
			}

		});
		
		
		//Button1
				Button dirChooserButton1 = (Button) findViewById(R.id.btnFile);
				dirChooserButton1.setOnClickListener(new OnClickListener() 
				{
					String m_chosen;
					@Override
					public void onClick(View v) {
						
						EditText et_w = (EditText)findViewById(R.id.et_w);
						EditText et_h = (EditText)findViewById(R.id.et_h);
						EditText et_num = (EditText)findViewById(R.id.frame_num);
						EditText et_time = (EditText)findViewById(R.id.et_time);
						
						if(et_w.length()==0||et_w.equals("")||et_h.length()==0||et_h.equals("")||et_num.length()==0||et_num.equals("")){
							Toast.makeText(MyAndroidAppActivity.this, " Please input width and height first ",10000).show();
						}else{
						mWidth=Integer.parseInt(et_w.getText().toString());
						mHeight=Integer.parseInt(et_h.getText().toString());
						num_frame=Integer.parseInt(et_num.getText().toString());
						timeElapsed=Integer.parseInt(et_time.getText().toString());
				//		input_one_buf= new byte[ mWidth * mHeight * 3 / 2];
						input_data=new byte[ num_frame*mWidth * mHeight * 3 / 2];
			//			Toast.makeText(MyAndroidAppActivity.this, " width is:" + mWidth + ", input height is:" + mHeight+",read "+num_frame+" frames input,encode for:"+timeElapsed+" seconds,input buf:"+input_data.length,5000).show();
						/////////////////////////////////////////////////////////////////////////////////////////////////
						//Create FileOpenDialog and register a callback
						/////////////////////////////////////////////////////////////////////////////////////////////////
						SimpleFileDialog FileOpenDialog;
						 

							FileOpenDialog = new SimpleFileDialog(
									MyAndroidAppActivity.this,
									"FileOpen",
									new SimpleFileDialog.SimpleFileDialogListener() {
										@Override
										public void onChosenDir(String chosenDir) {
											// The code in this function will be executed when the dialog OK button is pushed 
											m_chosen = chosenDir;
											Toast.makeText(
													MyAndroidAppActivity.this,
													"Chosen FileOpenDialog File: "
															+ m_chosen,
													Toast.LENGTH_LONG).show();

												mfilename = m_chosen;
												if (mfilename == "")
													Log.d(TAG,
															"null input filename");
												if (READ_FILE) {
													try {
														Log.e(TAG,
																"new fileinputstream");
														inFile = new FileInputStream(
																mfilename);
													} catch (IOException ioe) {
														Log.e(TAG,
																"Unable to read input file "
																		+ mfilename);
														throw new RuntimeException(
																ioe);
													}
												}
												if (READ_FILE) {
													circleProgressBar.setVisibility(View.VISIBLE);

													circleProgressBar.setProgress(0);
													//bytesRead=readYUV(inFile,frameData,inputFrameIndex);
													
													//new Thread(new thread_read(inFile, input_data,mWidth * mHeight* 3 / 2)).start();
												//	Log.d(TAG, "input_one_buf length:"+input_one_buf.length);

												//	read_one(inFile,input_one_buf,mWidth * mHeight* 3 / 2);
												//	Log.d(TAG, "read input data done, input data length:"+input_one_buf.length+",mWidth * mHeight* 3 / 2="+(mWidth * mHeight* 3 / 2));
													Log.d(TAG, "read input data, input buffer length:"+input_data.length+",input data:"+(mWidth * mHeight* 3 / 2*num_frame));

													read_all(inFile, input_data,mWidth * mHeight* 3 / 2*num_frame);
													circleProgressBar.setVisibility(View.GONE);
													

												}
											
										}
									});
						
						//You can change the default filename using the public variable "Default_File_Name"
						FileOpenDialog.Default_File_Name = "";
						FileOpenDialog.chooseFile_or_Dir();
		
					
					if (false) {
						FileOutputStream outputStream = null;
						if (DEBUG_SAVE_FILE) {
							String fileName = DEBUG_FILE_NAME_BASE + "input"
									+ mWidth + "x" + mHeight + ".yv12";
							try {
								outputStream = new FileOutputStream(fileName);
								Log.d(TAG, "encoded output will be saved as "
										+ fileName);
							} catch (IOException ioe) {
								Log.e(TAG, "Unable to create debug output file "
										+ fileName);
								throw new RuntimeException(ioe);
							}
						}
						try {
							Log.d(TAG, "write the input data ");
							outputStream.write(input_data, 0, (frameCount + 1)
									* mWidth * mHeight * 3 / 2);

							//outputFrameIndex++;
						} catch (IOException ioe) {
							Log.w(TAG, "failed writing debug data to file");
							throw new RuntimeException(ioe);
						}
					}
						
						}
					}
					
				});
	
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // TODO Auto-generated method stub
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == REQUESTCODE_PICK_VIDEO
	            && resultCode == Activity.RESULT_OK) {
	        Uri yuvUri = data.getData();
	        Log.d("", "Video URI= " + yuvUri);
	        Toast.makeText(MyAndroidAppActivity.this,  "Video URI= " + yuvUri, 5000);

	    }
	}
	
    Handler handler = new Handler() {
		  @Override
			public void handleMessage(Message msg) {			  
				Bundle bundle = msg.getData();
				String string = bundle.getString("readStatus");
				if("ReadDone"==string){
					Log.d(TAG, "read done, msg received, total frame read:"+frameCount);
					circleProgressBar.setVisibility(View.GONE);
				}

				int what=msg.what;
				int eos=msg.arg1;
				int buf_index=msg.arg1;
                switch (what) {
                    case 1:
                        mCircEncoder.feedInputbuf(0,buf_index);
                        if(!Thread.currentThread().isInterrupted()){
                        	Log.d(TAG, "vin:send "+msg.arg2+" frame to encoder");
                        	rectProgressBar.setProgress(msg.arg2);
                        }
                        break;
                    case 2:
                    	Toast.makeText(MyAndroidAppActivity.this, "Encoding Complete,encode time:"+msg.arg2+" ms", 10000).show();
        				rectProgressBar.setVisibility(View.GONE);
                    	long time1=System.currentTimeMillis();
                 //   	mCircEncoder.shutdown();
                    	Log.d(TAG, "vin:shutdown time:"+(System.currentTimeMillis()-time1));
                        if (inFile != null) {
                            try {
                                inFile.close(); 
                            } catch (IOException ioe) {
                                Log.w(TAG, "failed closing input file");
                                throw new RuntimeException(ioe);
                            }
                        }
        				
        				Thread.currentThread().interrupt();
                    	break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
			}
	};
	
/*	
	public class thread_read implements Runnable{
		private FileInputStream inFile;
		private byte[] YUV;
		private int size;

		public thread_read(FileInputStream in, byte[] enData, int out){
			inFile=in;
			YUV=enData;
			size=out;
		}

		@Override
		public void run() {
			Message msg = handler.obtainMessage();
			long t_i = System.nanoTime();
			frameCount = read_all(
					inFile, YUV,
					size);
		//	Log.d(TAG, "read done, total frame:" +frameCount);
			long t_o = System.nanoTime();
			long frame_timeUs = t_o- t_i;
			Log.d(TAG,"*******time for reading "+frameCount+" frame is: "
							+ frame_timeUs
							/ 1E6
							+ "ms");
			Bundle bundle = new Bundle();
			bundle.putString("readStatus", "ReadDone");
			msg.setData(bundle);
			handler.sendMessage(msg);
			
		}
		
		
	}
	*/
	public class on_encode_thread implements Runnable{

		private int num_frame,mfps,last_frame;

		public on_encode_thread(int frame_num,int fps){
			num_frame=frame_num;
			mfps=fps;
		}
		public on_encode_thread(int frame_num,int fps,int frame_tail){
			num_frame=frame_num;
			mfps=fps;
			last_frame=frame_tail;
		}
		@Override
		public void run() {
			long time_s=System.currentTimeMillis();
		//	for(int i=0 ; i <num_frame; i++){
			int i=0,j=0;
			try {
		
			boolean loop=true; 
			myloop: while(loop){
				for(int p=0;p<last_frame;p++){
					
						if(i>=num_frame){
							loop=false;
							break myloop;
						}
						if(mfps==30)Thread.sleep(33);
						else if(mfps==60)Thread.sleep(16);
						else if(mfps==120)Thread.sleep(8);
						Log.d(TAG, "vin:timestamp:"+System.currentTimeMillis());
						Message msg = handler.obtainMessage();
						msg.what=1;
						msg.arg1=p;//the frame index
						
						i++;
						msg.arg2=i;
					//	if(i==(num_frame-1))msg.arg1=1;
						handler.sendMessage(msg);
					} 
				}
		//	}
			}catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long time_elapse=System.currentTimeMillis()-time_s;
			Log.d(TAG, "vin:total frame sent:"+i+",time elapsed:"+time_elapse);
			Message msg = handler.obtainMessage();
			msg.what=2;
			msg.arg2=(int)time_elapse;
			handler.sendMessage(msg);
			
		}
		
		
	}
    
	public class encode_thread implements Runnable{
		private MediaCodec encoder;
		int index;long time_interval; int bytesRead;long presentationTimeUs; int ee;int frame_index;
		public encode_thread(int arg0, long arg1, int arg2, long arg3, int arg4,int frame_i,MediaCodec enc){
			index=arg0;
			bytesRead=arg2;
			presentationTimeUs=arg3;
			ee=arg4;
			time_interval=arg1;
			frame_index=frame_i;
			encoder=enc;
		}

		@Override
		public void run() {
			try {
				long time_s=33-time_interval;
				Log.d(TAG, "encoding input frame "+frame_index+ "frame interval:" +time_interval+", thread sleep for "+time_s);
				if(time_s>=0)Thread.sleep(time_s);
					
				Log.d(TAG, "index:"+index+",bytesRead:"+bytesRead+",PTS:"+presentationTimeUs+",Inputframe:"+frame_index+",flag:"+ee);
				encoder.queueInputBuffer(
		                index,
		                0,  // offset
		                bytesRead,  // size
		                presentationTimeUs,
		                ee);  
					
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Cannot dequeue Input buffer " + e.toString());
			}
	}

	}
	public class MyThread implements Runnable{
		private MediaCodec.BufferInfo info;
		private ByteBuffer encodedData;
		private FileOutputStream outputStream;
		private int frame_index;
		public MyThread(MediaCodec.BufferInfo in,ByteBuffer enData,FileOutputStream out, int frameIndex){
			info=in;
			encodedData=enData;
			outputStream=out;
			frame_index=frameIndex;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			long t_i=System.nanoTime();
           // byte[] data = new byte[info.size];
			 byte[] data = new byte[encodedData.remaining()];
   //         encodedData.remaining();
            Log.d(TAG, " frame: "+frame_index+",info size: " + info.size +"encodedData remaining: "+ encodedData.remaining());
            encodedData.get(data);
            encodedData.position(info.offset);
            try {
                outputStream.write(data);
                //outputFrameIndex++;
            } catch (IOException ioe) {
                Log.w(TAG, "failed writing debug data to file");
                throw new RuntimeException(ioe);
            }
          	 long t_o=System.nanoTime();
           	 long frame_timeUs=t_o-t_i;
           	 Log.d(TAG, "*******time for writing frame " + frame_index + ": "+frame_timeUs/1E6 + "ms");
			
		}
		
		
	}

private void encode2(int width, int height, int framerate, boolean hw,String mime,int colorFormat){
    int err;
    
    MediaFormat format=MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
//    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

    format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
    MediaCodec encoder = null;
    FileInputStream inFile=null;
    if(READ_FILE){
    	try{
    		Log.e(TAG,"new fileinputstream");
    		inFile=new FileInputStream(mfilename);
    	}catch(IOException ioe) {
            Log.e(TAG, "Unable to read input file " + mfilename);
            throw new RuntimeException(ioe);
        }
    }
    // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
    // stream, not a .mp4 file, so not all players will know what to do with it.
    FileOutputStream outputStream = null;
    if (DEBUG_SAVE_FILE) {
        String fileName = DEBUG_FILE_NAME_BASE + mWidth + "x" + mHeight + ".h264";
        try {
            outputStream = new FileOutputStream(fileName);
            Log.d(TAG, "encoded output will be saved as " + fileName);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to create debug output file " + fileName);
            throw new RuntimeException(ioe);
        }
    }
 //   Time t_start = new Time();
 //   t_start.setToNow();
    long t_begin=System.nanoTime();
    if (hw)
    {
        encoder = MediaCodec.createEncoderByType(MIME_TYPE);

    }
    else
    {
        encoder = MediaCodec.createEncoderByType(MIME_TYPE);
//        codec = MediaCodec::CreateByComponentName(looper,"OMX.google.h264.encoder");
    }
    
    if (encoder == null) {
        Log.e(TAG, "ERROR: unable to create VP8 codec instance\n");
        
    }
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


    Log.d(TAG, "Starting encode codec");
    encoder.start();

    
    final int TIMEOUT_USEC = 10000;
    ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
    ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
    
    int iteration=10;
    int total_in=0;
    int total_out=0;
    long frame_n=0;
    long frame_last=0;
    boolean first_loop=true;
    int bad_frame=0;
    for(int p=0;p<iteration;p++){
	    Log.d(TAG, "**********iteration: " + p);
	    long presentationTimeUs = 0;
	    int inputFrameIndex = 0;
	    int outputFrameIndex = 0;
	    boolean sawInputEOS = false;
	    boolean sawOutputEOS = false;
	    final int kTimeout=10000;
	    byte[] frameData = new byte[mWidth * mHeight * 3 / 2];
	    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
	    int frameSize = width * height * 3 / 2;
	    long encodedSize = 0;
	    boolean encoderDone = false;
	    // encode loop
	    long frame_timeUs=0;
	    long frame_s=0;
	    long frame_e=0;
	    long frame_interval=10;
	    long t_i=0;
	    long t_o=0;
		boolean READALL=true;
		
		Log.d(TAG, "****frame count " + frameCount);
	
	    while(!sawOutputEOS) {
	    	Log.d(TAG, "**********loop******iteration: "+p);
	        
			
	    	if(!READALL){
	       //handle input
	        if (!sawInputEOS) {
	            int index;
	            index = encoder.dequeueInputBuffer(kTimeout);
	            if (index>=0) {
	                int bytesRead;
	                ByteBuffer buffer = encoderInputBuffers[index];
	                
	                bytesRead = buffer.capacity();
	                int tt=0;
	                if (READ_FILE){
	                	//bytesRead=readYUV(inFile,frameData,inputFrameIndex);
	                	 t_i=System.nanoTime();
	                	tt=read_t(inFile, frameData, frameSize);
	                	 t_o=System.nanoTime();
	                	 frame_timeUs=t_o-t_i;
	                	 Log.d(TAG, "*******time for reading 1 frame:" + frame_timeUs/1E6 + "ms");
	                	 
	                }
	                if (tt<0) {
	                    
	                    //        memset(buffer->data(),0,buffer->size());
	                        	Log.d(TAG,"setting EOS");
	                            sawInputEOS = true;
	                            bytesRead = 0;
	                        }
	                if (bytesRead < buffer.capacity()) {
	                
	            //        memset(buffer->data(),0,buffer->size());
	                	Log.d(TAG,"setting EOS");
	                    sawInputEOS = true;
	                    bytesRead = 0;
	                }	
	                
	                buffer.clear();
	                buffer.put(frameData);
	                if(inputFrameIndex >= MAX_ENCODE_FRAMES && !READ_FILE){
	                    sawInputEOS = true;
	                    bytesRead = 0;
	                }
	
	                presentationTimeUs = (inputFrameIndex * 1000000) / framerate;
	                 
	                encoder.queueInputBuffer(
	                        index,
	                        0,  // offset
	                        bytesRead,  // size
	                        presentationTimeUs,
	                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);  
	                       inputFrameIndex++;
	                       frame_s=System.nanoTime();
	
	            }
	        }
	    	}
	    	
	    	if(READALL){
	
	              if (false) {
					try {
						Log.d(TAG, "write the " + inputFrameIndex
								+ "frame, with offset " + inputFrameIndex * mWidth
								* mHeight * 3 / 2 + "size " + mWidth * mHeight * 3
								/ 2);
						outputStream.write(input_data, inputFrameIndex * mWidth
								* mHeight * 3 / 2, mWidth * mHeight * 3 / 2);
	
						//outputFrameIndex++;
					} catch (IOException ioe) {
						Log.w(TAG, "failed writing debug data to file");
						throw new RuntimeException(ioe);
					}
				}
				if(inputFrameIndex>=frameCount) sawInputEOS=true;
		    		if (!sawInputEOS) {
		    			int index;
		                index = encoder.dequeueInputBuffer(kTimeout);
		                if (index>=0) {
		                    int bytesRead;
		                    ByteBuffer buffer = encoderInputBuffers[index];
		                    
		                    bytesRead = buffer.capacity();
		                    buffer.clear();
		             //       buffer.put(input_data[inputFrameIndex]);
		                    Log.d(TAG, "put the " + inputFrameIndex + "frame");
		                    buffer.put(input_data, inputFrameIndex*mWidth*mHeight*3/2, mWidth*mHeight*3/2);
		                    presentationTimeUs = (inputFrameIndex * 1000000) / framerate;
		                    if(first_loop){
	   		    				frame_n=System.nanoTime();
	   		    				frame_last=frame_n;
	   		    				first_loop=false;
	   		    			}
	   		    			else{
	   		    				frame_n=System.nanoTime();
	   		    			}
	                           if(inputFrameIndex>=frameCount) sawInputEOS=true;
	                           long frame_elapse=(long) ((frame_n-frame_last)/1E6);
	   		    			if(frame_elapse>33){
	   		    				bad_frame++;
	   		    				Log.e(TAG, bad_frame+" bad frame found");
	   		    			}else{
			   		    		try {
									Thread.sleep(33-frame_elapse);
									
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	   		    			}
	   		    			Log.d(TAG, "******vin: input_frame:"+total_in+"time interval: "+frame_elapse+",thread slept for:"+(33-frame_elapse)+" ms");
	   		    			frame_last=frame_n;
				      //      new Thread(new encode_thread(index,frame_elapse,bytesRead,presentationTimeUs,sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0,total_in,encoder)).start();
	   		    			encoder.queueInputBuffer(
	   				                index,
	   				                0,  // offset
	   				                bytesRead,  // size
	   				                presentationTimeUs,
	   				             sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);  
		                           frame_s=System.nanoTime();
		                           Log.d(TAG, "***queue input buffer: " + inputFrameIndex);
		                           inputFrameIndex++;
		                           total_in++;
		                }else{
		                	Log.e(TAG, "Cannot dequeue input buffer");
		                }
		            }
	    		
	    	}
	
	
	
	        //handle output buffers
	        int index,offset,size;
	
	        int flags;
	        int encoderStatus=encoder.dequeueOutputBuffer(info,kTimeout);
	
	 
	//question about the encoderstatus and index
	        if(encoderStatus>=0){ // encoderStatus >= 0
	        	if (info.size > 0)  
	        	{
	        		
	                if (presentationTimeUs == 0) {
	                    presentationTimeUs = System.currentTimeMillis()/1000;
	                }
	                 frame_e=System.nanoTime();
	                 frame_timeUs=frame_e-frame_s;
	                 Log.d(TAG, "*******time for encoding " + outputFrameIndex + " frame:" + frame_timeUs/1E6 + "ms");
	                 
	                 /////////////add the encoding time check here????//////////////////
	                 
	                 
	                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
	                if (encodedData == null) {
	                   	Log.e(TAG,"encoderOutputBuffer was null");
	                    
	                }
	
	                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
	                encodedData.position(info.offset);
	                encodedData.limit(info.offset + info.size);
	
	                encodedSize += info.size;
	                if (outputStream != null) {
	                	
	                	new Thread(new MyThread(info, encodedData, outputStream,outputFrameIndex)).start();
	                	 outputFrameIndex++;
	                	 total_out++;
	     	            if(READALL){
	    	            	if(outputFrameIndex>=frameCount) sawOutputEOS=true;
	    	            }
	                	 
	                }
	                if (encoderStatus== MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
	                {
	           		 if (VERBOSE) Log.d(TAG, "MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
	           		  format =MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
	                    format.setByteBuffer("csd-0", encodedData);
	                    
	               }

	            encoder.releaseOutputBuffer(encoderStatus, false);
	    //        encoderStatus=encoder.dequeueOutputBuffer(info,kTimeout);
	            
	        		}    
	        }else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
			      // no output available yet
			      if (VERBOSE) Log.d(TAG, "no output from encoder available");
			  } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			      // not expected for an encoder
			  	encoderOutputBuffers = encoder.getOutputBuffers();
			      if (VERBOSE) Log.d(TAG, "encoder output buffers changed");
			  } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			      // not expected for an encoder
			      MediaFormat newFormat = encoder.getOutputFormat();
			      if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);
			  }    
	            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
	                Log.d(TAG,"Received end-of-stream");
	
	                sawOutputEOS = true;
	            }

	
	
	    }
	    //reset
	    sawOutputEOS=false;
	    inputFrameIndex=0;
	    outputFrameIndex=0;
	    sawInputEOS=false;
	    
	}
   
 //       Time t_end = new Time();
 //       t_end.setToNow();
    long t_end=System.nanoTime();
   long elapsedTimeUs = t_end- t_begin;
    if(encoder != null){
	    encoder.stop();
	    encoder.release();
    }
       
    if (outputStream != null) {
        try {
            outputStream.close();
        } catch (IOException ioe) {
            Log.w(TAG, "failed closing debug file");
            throw new RuntimeException(ioe);
        }
    }
    
    if (inFile != null) {
        try {
            inFile.close(); 
        } catch (IOException ioe) {
            Log.w(TAG, "failed closing input file");
            throw new RuntimeException(ioe);
        }
    }
    Log.d(TAG, "****COMPLETE:encoded: " +total_in + "frames, write: "+total_out+"frames, with "+bad_frame+" bad frames");
//   Log.d(TAG,"encoding done, encoded " + total_in + "Frame in " + elapsedTimeUs / 1E6 + "ms," + (outputFrameIndex - 1) / (elapsedTimeUs / 1E6) + "fps");
  // Toast.makeText(MyAndroidAppActivity.this, "encoding done, encoded " + inputFrameIndex + "Frame in " + elapsedTimeUs / 1E6 + "seconds," + (outputFrameIndex - 1) / (elapsedTimeUs / 1E6) + "fps" ,5000).show();
}


}