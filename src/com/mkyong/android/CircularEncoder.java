/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mkyong.android;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import com.mkyong.android.MyAndroidAppActivity.MyThread;

/**
 * Encodes video in a fixed-size circular buffer.
 * <p>
 * The obvious way to do this would be to store each packet in its own buffer and hook it
 * into a linked list.  The trouble with this approach is that it requires constant
 * allocation, which means we'll be driving the GC to distraction as the frame rate and
 * bit rate increase.  Instead we create fixed-size pools for video data and metadata,
 * which requires a bit more work for us but avoids allocations in the steady state.
 * <p>
 * Video must always start with a sync frame (a/k/a key frame, a/k/a I-frame).  When the
 * circular buffer wraps around, we either need to delete all of the data between the frame at
 * the head of the list and the next sync frame, or have the file save function know that
 * it needs to scan forward for a sync frame before it can start saving data.
 * <p>
 * When we're told to save a snapshot, we create a MediaMuxer, write all the frames out,
 * and then go back to what we were doing.
 */
public class CircularEncoder {
    private static final String TAG = "EncoderTest";
    private static final boolean VERBOSE = true;
    private static final boolean READ_FILE = true;
    private static boolean DEBUG_SAVE_FILE = false;
    private static final String DEBUG_FILE_NAME_BASE = "/sdcard/Movies/output.";
    private static int inputFrameIndex=0;
    private static  String mfilename="";
    private static int width, height;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 10;           // sync frame every second
    private static final int FRAME_RATE=30;
    private EncoderThread mEncoderThread;
    private Surface mInputSurface;
    private MediaCodec encoder;
//    private static byte[] frameData=new byte[155520];
    private static  FileOutputStream outputStream = null;
    /**
     * Callback function definitions.  CircularEncoder caller must provide one.
     */
    public interface Callback {
        /**
         * Called some time after saveVideo(), when all data has been written to the
         * output file.
         *
         * @param status Zero means success, nonzero indicates failure.
         */
        void fileSaveComplete(int status);

        /**
         * Called occasionally.
         *
         * @param totalTimeMsec Total length, in milliseconds, of buffered video.
         */
        void bufferStatus(long totalTimeMsec);
    }

    /**
     * Configures encoder, and prepares the input Surface.
     *
     * @param width Width of encoded video, in pixels.  Should be a multiple of 16.
     * @param height Height of encoded video, in pixels.  Usually a multiple of 16 (1080 is ok).
     * @param bitRate Target bit rate, in bits.
     * @param frameRate Expected frame rate.
     * @param desiredSpanSec How many seconds of video we want to have in our buffer at any time.
     */
    public CircularEncoder(int mWidth, int mHeight, int mBitRate, int FRAME_RATE, int colorFormat,
    	       byte[] input ) throws IOException {
        // The goal is to size the buffer so that we can accumulate N seconds worth of video,
        // where N is passed in as "desiredSpanSec".  If the codec generates data at roughly
        // the requested bit rate, we can compute it as time * bitRate / bitsPerByte.
        //
        // Sync frames will appear every (frameRate * IFRAME_INTERVAL) frames.  If the frame
        // rate is higher or lower than expected, various calculations may not work out right.
        //
        // Since we have to start muxing from a sync frame, we want to ensure that there's
        // room for at least one full GOP in the buffer, preferrably two.
  
    	 MediaFormat format=MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
//       format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
       format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

       format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
       format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
       format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
       MediaCodec encoder = null;
       FileInputStream inFile=null;
  //     frameData=input;
     //  frameData=input;
/*       if(READ_FILE){
       	try{
       		Log.e(TAG,"new fileinputstream");
       		inFile=new FileInputStream(mfilename);
       	}catch(IOException ioe) {
               Log.e(TAG, "Unable to read input file " + mfilename);
               throw new RuntimeException(ioe);
           }
       }
       */

       // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
       // stream, not a .mp4 file, so not all players will know what to do with it.
      
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

       if (false) {
			try {
				Log.d(TAG, "write the " + inputFrameIndex
						+ "frame, with offset " + inputFrameIndex * mWidth
						* mHeight * 3 / 2 + "size " + mWidth * mHeight * 3
						/ 2);
				outputStream.write(input, 0, mWidth * mHeight * 3 / 2);

				//outputFrameIndex++;
			} catch (IOException ioe) {
				Log.w(TAG, "failed writing debug data to file");
				throw new RuntimeException(ioe);
			}
		}
    //   Time t_start = new Time();
    //   t_start.setToNow();
       long t_begin=System.nanoTime();

       
           encoder = MediaCodec.createEncoderByType(MIME_TYPE);
//           codec = MediaCodec::CreateByComponentName(looper,"OMX.google.h264.encoder");
       
       
       if (encoder == null) {
           Log.e(TAG, "ERROR: unable to create encoder instance\n");
           
       }
       encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


       Log.d(TAG, "Starting encode codec");
       encoder.start();

        // Start the encoder thread last.  That way we're sure it can see all of the state
        // we've initialized.
 //       mEncoderThread = new EncoderThread(mEncoder, encBuffer, cb);
        mEncoderThread = new EncoderThread(encoder,input,mWidth,mHeight);

        mEncoderThread.start();
        
        mEncoderThread.waitUntilReady();
    }
    public CircularEncoder(String mime, int mWidth, int mHeight, int mBitRate, int FRAME_RATE, int i_interval,int colorFormat,
 	       byte[] input,int num_frame, boolean flag_dump ) throws IOException {
     // The goal is to size the buffer so that we can accumulate N seconds worth of video,
     // where N is passed in as "desiredSpanSec".  If the codec generates data at roughly
     // the requested bit rate, we can compute it as time * bitRate / bitsPerByte.
     //
     // Sync frames will appear every (frameRate * IFRAME_INTERVAL) frames.  If the frame
     // rate is higher or lower than expected, various calculations may not work out right.
     //
     // Since we have to start muxing from a sync frame, we want to ensure that there's
     // room for at least one full GOP in the buffer, preferrably two.

 	 MediaFormat format=MediaFormat.createVideoFormat(mime, mWidth, mHeight);
//    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

    format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
    format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, i_interval);
    MediaCodec encoder = null;
    FileInputStream inFile=null;
    DEBUG_SAVE_FILE=flag_dump;
//     frameData=input;
  //  frameData=input;
/*       if(READ_FILE){
    	try{
    		Log.e(TAG,"new fileinputstream");
    		inFile=new FileInputStream(mfilename);
    	}catch(IOException ioe) {
            Log.e(TAG, "Unable to read input file " + mfilename);
            throw new RuntimeException(ioe);
        }
    }
    */

    // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
    // stream, not a .mp4 file, so not all players will know what to do with it.
   
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

    if (false) {
			try {
				Log.d(TAG, "write the " + inputFrameIndex
						+ "frame, with offset " + inputFrameIndex * mWidth
						* mHeight * 3 / 2 + "size " + mWidth * mHeight * 3
						/ 2);
				outputStream.write(input, 0, mWidth * mHeight * 3 / 2);

				//outputFrameIndex++;
			} catch (IOException ioe) {
				Log.w(TAG, "failed writing debug data to file");
				throw new RuntimeException(ioe);
			}
		}
 //   Time t_start = new Time();
 //   t_start.setToNow();
    long t_begin=System.nanoTime();

    
        encoder = MediaCodec.createEncoderByType(mime);
//        codec = MediaCodec::CreateByComponentName(looper,"OMX.google.h264.encoder");
    
    
    if (encoder == null) {
        Log.e(TAG, "ERROR: unable to create encoder instance\n");
        
    }
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


    Log.d(TAG, "Starting encode codec");
    encoder.start();

     // Start the encoder thread last.  That way we're sure it can see all of the state
     // we've initialized.
//       mEncoderThread = new EncoderThread(mEncoder, encBuffer, cb);
     mEncoderThread = new EncoderThread(encoder,input,mWidth,mHeight,num_frame);

     mEncoderThread.start();
     
     mEncoderThread.waitUntilReady();
 }
    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Shuts down the encoder thread, and releases encoder resources.
     * <p>
     * Does not return until the encoder thread has stopped.
     */
    public void shutdown() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");

        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN));
        try {
            mEncoderThread.join();
        } catch (InterruptedException ie) {
            Log.w(TAG, "Encoder thread join() was interrupted", ie);
        }

        if (encoder != null) {
        	encoder.stop();
        	encoder.release();
        	encoder = null;
        	Log.d(TAG, "vin:release encoder");
        }else Log.d(TAG, "vin: null encoder");
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }
        

    }

    /**
     * Notifies the encoder thread that a new frame will shortly be provided to the encoder.
     * <p>
     * There may or may not yet be data available from the encoder output.  The encoder
     * has a fair mount of latency due to processing, and it may want to accumulate a
     * few additional buffers before producing output.  We just need to drain it regularly
     * to avoid a situation where the producer gets wedged up because there's no room for
     * additional frames.
     * <p>
     * If the caller sends the frame and then notifies us, it could get wedged up.  If it
     * notifies us first and then sends the frame, we guarantee that the output buffers
     * were emptied, and it will be impossible for a single additional frame to block
     * indefinitely.
     */
    public void frameAvailableSoon() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON));
    }
    public void feedInputbuf(int eos, int index){
	    Handler handler = mEncoderThread.getHandler();
	
	//	Message msg = handler.obtainMessage();
		Message msg=handler.obtainMessage(EncoderThread.EncoderHandler.MSG_FEED_ENCODER, eos, index);
	
		handler.sendMessage(msg);
		
}
    /**
     * Initiates saving the currently-buffered frames to the specified output file.  The
     * data will be written as a .mp4 file.  The call returns immediately.  When the file
     * save completes, the callback will be notified.
     * <p>
     * The file generation is performed on the encoder thread, which means we won't be
     * draining the output buffers while this runs.  It would be wise to stop submitting
     * frames during this time.
     */
    public void saveVideo(File outputFile) {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_SAVE_VIDEO, outputFile));
    }

    /**
     * Object that encapsulates the encoder thread.
     * <p>
     * We want to sleep until there's work to do.  We don't actually know when a new frame
     * arrives at the encoder, because the other thread is sending frames directly to the
     * input surface.  We will see data appear at the decoder output, so we can either use
     * an infinite timeout on dequeueOutputBuffer() or wait() on an object and require the
     * calling app wake us.  It's very useful to have all of the buffer management local to
     * this thread -- avoids synchronization -- so we want to do the file muxing in here.
     * So, it's best to sleep on an object and do something appropriate when awakened.
     * <p>
     * This class does not manage the MediaCodec encoder startup/shutdown.  The encoder
     * should be fully started before the thread is created, and not shut down until this
     * thread has been joined.
     */
    private static class EncoderThread extends Thread {
        private MediaCodec mEncoder;
        private MediaFormat mEncodedFormat;
        private MediaCodec.BufferInfo mBufferInfo;
        private byte[] frameData;
    	private ByteBuffer[] encoderInputBuffers ;
    	private ByteBuffer[] encoderOutputBuffers;
    	private int width,height;
        private EncoderHandler mHandler;
   //     private CircularEncoderBuffer mEncBuffer;
        private CircularEncoder.Callback mCallback;
        private int mFrameNum;
        byte[] input_data=null;

        private final Object mLock = new Object();
        private volatile boolean mReady = false;

        public EncoderThread(MediaCodec mediaCodec, byte[] input_buf, int mWidth, int mHeight) {
        	Log.d(TAG,"Entering EncoderThread*****input buf length:"+input_buf.length);
            mEncoder = mediaCodec;
      //      mEncBuffer = encBuffer;
      //      mCallback = callback;
            frameData=new byte[width*height*3/2];

            frameData=input_buf;
            mBufferInfo = new MediaCodec.BufferInfo();
        	 encoderInputBuffers = mEncoder.getInputBuffers();
        	 encoderOutputBuffers = mEncoder.getOutputBuffers();
        	  	width=mWidth;
            	height=mHeight;
            /*
            if (DEBUG_SAVE_FILE) {
                String fileName = "Input.yv12";
                try {
                    outputStream = new FileOutputStream(fileName);
                    Log.d(TAG, "encoded output will be saved as " + fileName);
                } catch (IOException ioe) {
                    Log.e(TAG, "Unable to create debug output file " + fileName);
                    throw new RuntimeException(ioe);
                }
            }
            if (true) {
    			try {
    				Log.d(TAG, "vin:encoderThread write the " + inputFrameIndex
    						+ "frame, with offset ");
    				outputStream.write(input_buf, 0, 360 * 288 * 3 / 2);

    				//outputFrameIndex++;
    			} catch (IOException ioe) {
    				Log.w(TAG, "failed writing debug data to file");
    				throw new RuntimeException(ioe);
    			}
    		}
    		*/
        }
        public EncoderThread(MediaCodec mediaCodec, byte[] input_buf, int mWidth, int mHeight,int frameNum) {
        	Log.d(TAG,"Entering EncoderThread*****input buf length:"+input_buf.length+",frame number:"+frameNum);
            mEncoder = mediaCodec;
      //      mEncBuffer = encBuffer;
      //      mCallback = callback;
            input_data = new byte[frameNum * mWidth * mHeight * 3 / 2];
            input_data=input_buf;
            mBufferInfo = new MediaCodec.BufferInfo();
        	 encoderInputBuffers = mEncoder.getInputBuffers();
        	 encoderOutputBuffers = mEncoder.getOutputBuffers();
        	  	width=mWidth;
            	height=mHeight;
            /*
            if (DEBUG_SAVE_FILE) {
                String fileName = "Input.yv12";
                try {
                    outputStream = new FileOutputStream(fileName);
                    Log.d(TAG, "encoded output will be saved as " + fileName);
                } catch (IOException ioe) {
                    Log.e(TAG, "Unable to create debug output file " + fileName);
                    throw new RuntimeException(ioe);
                }
            }
            if (true) {
    			try {
    				Log.d(TAG, "vin:encoderThread write the " + inputFrameIndex
    						+ "frame, with offset ");
    				outputStream.write(input_buf, 0, 360 * 288 * 3 / 2);

    				//outputFrameIndex++;
    			} catch (IOException ioe) {
    				Log.w(TAG, "failed writing debug data to file");
    				throw new RuntimeException(ioe);
    			}
    		}
    		*/
        }
        /**
         * Thread entry point.
         * <p>
         * Prepares the Looper, Handler, and signals anybody watching that we're ready to go.
         */
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new EncoderHandler(this);    // must create on encoder thread
            Log.d(TAG, "encoder thread ready");
            synchronized (mLock) {
                mReady = true;
                mLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();

            synchronized (mLock) {
                mReady = false;
                mHandler = null;
            }
            Log.d(TAG, "looper quit");
        }

        /**
         * Waits until the encoder thread is ready to receive messages.
         * <p>
         * Call from non-encoder thread.
         */
        public void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        public EncoderHandler getHandler() {
            synchronized (mLock) {
                // Confirm ready state.
                if (!mReady) {
                    throw new RuntimeException("not ready");
                }
            }
            return mHandler;
        }

        public void feedEncoder(boolean sawInputEOS, int inputFrameIndex){
        	Log.d(TAG,"vin: EncoderThread feedEncoder*****");

    	    final int kTimeout=10000;

        	while (true) {
    			int index;
                index = mEncoder.dequeueInputBuffer(10000);
                if (index>=0) {
                	Log.d(TAG, "dequeue index:"+index);
                    int bytesRead;
                    ByteBuffer buffer = encoderInputBuffers[index];
                    bytesRead = buffer.capacity();
                    buffer.clear();
                    //Log.d(TAG, "put the " + inputFrameIndex + "frame,buffer remaining:"+buffer.remaining()+",input data length:"+frameData.length);
                    Log.d(TAG, "put the " + inputFrameIndex + "frame,buffer remaining:"+buffer.remaining()+",input data length:"+input_data.length);

                    buffer.put(input_data, inputFrameIndex*width*height*3/2, width*height*3/2);
             //       buffer.put(frameData); //one frame
                  /*  
                    if (DEBUG_SAVE_FILE) {
                        String fileName = "/sdcard/Movies/Input.nv12";
                        try {
                            outputStream = new FileOutputStream(fileName);
                            Log.d(TAG, "encoded output will be saved as " + fileName);
                        } catch (IOException ioe) {
                            Log.e(TAG, "Unable to create debug output file " + fileName);
                            throw new RuntimeException(ioe);
                        }
                    }
                    if (true) {
            			try {
            				Log.d(TAG, "vin:encoderThread write the " + inputFrameIndex
            						+ "frame, with offset ");
            				outputStream.write(input_data, inputFrameIndex*width*height*3/2, width*height*3/2);
            				//outputFrameIndex++;
            			} catch (IOException ioe) {
            				Log.w(TAG, "failed writing debug data to file");
            				throw new RuntimeException(ioe);
            			}
            		}
                    
                  */  
                    long presentationTimeUs = (inputFrameIndex * 1000000) / FRAME_RATE;
                    mEncoder.queueInputBuffer(
				                index,
				                0,  // offset
				                bytesRead,  // size
				                presentationTimeUs,
				             sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);  
                         
                           Log.d(TAG, "***queue input buffer: " + inputFrameIndex+",buffer index:"+index);
                           inputFrameIndex++;
                           break;
                       
                }else{
                	Log.e(TAG, "Cannot dequeue input buffer,try again, index:"+index);
                }
            } 
//drainEncoder();//////////////////////////////////////
          	while(true){
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, 10000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                	 if (VERBOSE) Log.d(TAG, "vin:no output from encoder available");
                    
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "vin:encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    mEncodedFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "vin:encoder output format changed: " + mEncodedFormat);
                } else if (encoderStatus < 0) {
                    Log.d(TAG, "vin:unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                	mEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }
                    if (mBufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        Log.d(TAG, "encodedData.remaining():"+encodedData.remaining());
                   	 byte[] data = new byte[mBufferInfo.size];
                   	encodedData.get(data);
                    Log.d(TAG, " frame: "+mFrameNum+",info size: " + mBufferInfo.size +",encodedData remaining: "+ encodedData.remaining()+",mBufferInfo.offset:"+mBufferInfo.offset+",data length:"+data.length);
                    if(DEBUG_SAVE_FILE){
                        if (outputStream != null) {

                             try {
                            	 Log.d(TAG, "vin:write to disk, data length:"+data.length);
                                   outputStream.write(data);
                              } catch (IOException ioe) {
                            	  		Log.w(TAG, "failed writing debug data to file");
                                          throw new RuntimeException(ioe);
                               }
    	                	 mFrameNum++;
    	     	               	                	 
    	                }
                    }
                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        }
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out when we got the
                            // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                            // a single big blob -- it wants separate csd-0/csd-1 chunks --
                            // so simply saving this off won't work.
                            if (VERBOSE) Log.d(TAG, "vin:ignoring BUFFER_FLAG_CODEC_CONFIG");
                       //     mBufferInfo.size = 0;
        
                            mEncodedFormat =MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                           mEncodedFormat.setByteBuffer("csd-0", encodedData);
                        }
                        mEncoder.releaseOutputBuffer(encoderStatus, false);

                    }
          

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                      //  break;      // out of while
                    }
                    break;
                }
    	}
        	//////////////////////////////////////////////
        }
        
        public void drainEncoder(){
        	while(true){
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, 10000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                	 if (VERBOSE) Log.d(TAG, "vin:no output from encoder available");
                    
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "vin:encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    mEncodedFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "vin:encoder output format changed: " + mEncodedFormat);
                } else if (encoderStatus < 0) {
                    Log.d(TAG, "vin:unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                	mEncoder.dequeueOutputBuffer(mBufferInfo, 1000);
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }
                    if (mBufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        Log.d(TAG, "encodedData.remaining():"+encodedData.remaining());
                   	 byte[] data = new byte[mBufferInfo.size];
                   	encodedData.get(data);
                    Log.d(TAG, " frame: "+mFrameNum+",info size: " + mBufferInfo.size +",encodedData remaining: "+ encodedData.remaining()+",mBufferInfo.offset:"+mBufferInfo.offset+",data length:"+data.length);

    //write to disk *vin
                     //   mEncBuffer.add(encodedData, mBufferInfo.flags,
                       //         mBufferInfo.presentationTimeUs);
                        if (outputStream != null) {
    	             /*   	
    	                	//new Thread(new MyThread(mBufferInfo, encodedData, outputStream,mFrameNum)).start();
                        	 byte[] data = new byte[encodedData.remaining()];
                             Log.d(TAG, " frame: "+mFrameNum+",info size: " + mBufferInfo.size +",encodedData remaining: "+ encodedData.remaining()+",mBufferInfo.offset:"+mBufferInfo.offset);
                            
                             encodedData.position(mBufferInfo.offset);
                             encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                             encodedData.get(data);
         	    //           
         	    //           encodedData.get(data);
         	              encodedData.position(mBufferInfo.offset);
         	              */
                             try {
                                   outputStream.write(data);
                              } catch (IOException ioe) {
                            	  		Log.w(TAG, "failed writing debug data to file");
                                          throw new RuntimeException(ioe);
                               }
    	                	 mFrameNum++;
    	     	               	                	 
    	                }

                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        }
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out when we got the
                            // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                            // a single big blob -- it wants separate csd-0/csd-1 chunks --
                            // so simply saving this off won't work.
                            if (VERBOSE) Log.d(TAG, "vin:ignoring BUFFER_FLAG_CODEC_CONFIG");
                       //     mBufferInfo.size = 0;
        
                            mEncodedFormat =MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                           mEncodedFormat.setByteBuffer("csd-0", encodedData);
                        }
                        mEncoder.releaseOutputBuffer(encoderStatus, false);

                    }
          

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                      //  break;      // out of while
                    }
                    break;
                }
    	}//while(true)	
   }
        /**
         * Drains all pending output from the decoder, and adds it to the circular buffer.
         */
        
        public void drainEncoder_bk() {
            final int TIMEOUT_USEC = 10000;     // no timeout -- check for buffers, bail if none
        	Log.d(TAG,"vin: EncoderThread drainEncoder*****");

            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                	 if (VERBOSE) Log.d(TAG, "vin:no output from encoder available");
                    
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "vin:encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    mEncodedFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "vin:encoder output format changed: " + mEncodedFormat);
                } else if (encoderStatus < 0) {
                    Log.d(TAG, "vin:unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }
                    if (mBufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                        Log.d(TAG, "encodedData.remaining():"+encodedData.remaining());
                   	 byte[] data = new byte[mBufferInfo.size];
                   	encodedData.get(data);
                    Log.d(TAG, " frame: "+mFrameNum+",info size: " + mBufferInfo.size +",encodedData remaining: "+ encodedData.remaining()+",mBufferInfo.offset:"+mBufferInfo.offset+",data length:"+data.length);

//write to disk *vin
                     //   mEncBuffer.add(encodedData, mBufferInfo.flags,
                       //         mBufferInfo.presentationTimeUs);
                        if (outputStream != null) {
    	             /*   	
    	                	//new Thread(new MyThread(mBufferInfo, encodedData, outputStream,mFrameNum)).start();
                        	 byte[] data = new byte[encodedData.remaining()];
                             Log.d(TAG, " frame: "+mFrameNum+",info size: " + mBufferInfo.size +",encodedData remaining: "+ encodedData.remaining()+",mBufferInfo.offset:"+mBufferInfo.offset);
                            
                             encodedData.position(mBufferInfo.offset);
                             encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                             encodedData.get(data);
         	    //           
         	    //           encodedData.get(data);
         	              encodedData.position(mBufferInfo.offset);
         	              */
                             try {
                                   outputStream.write(data);
                              } catch (IOException ioe) {
                            	  		Log.w(TAG, "failed writing debug data to file");
                                          throw new RuntimeException(ioe);
                               }
    	                	 mFrameNum++;
    	     	               	                	 
    	                }

                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                    mBufferInfo.presentationTimeUs);
                        }
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out when we got the
                            // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                            // a single big blob -- it wants separate csd-0/csd-1 chunks --
                            // so simply saving this off won't work.
                            if (VERBOSE) Log.d(TAG, "vin:ignoring BUFFER_FLAG_CODEC_CONFIG");
                       //     mBufferInfo.size = 0;
        
                            mEncodedFormat =MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                           mEncodedFormat.setByteBuffer("csd-0", encodedData);
                        }
                        mEncoder.releaseOutputBuffer(encoderStatus, false);

                    }
          

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                        break;      // out of while
                    }
                }
            }
        }
        
        /***
         * write to disk
         */
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
        /**
         * Drains the encoder output.
         * <p>
         * See notes for {@link CircularEncoder#frameAvailableSoon()}.
         */
        void frameAvailableSoon() {
            if (VERBOSE) Log.d(TAG, "frameAvailableSoon");
            drainEncoder();

            mFrameNum++;
            if ((mFrameNum % 10) == 0) {        // TODO: should base off frame rate or clock?
 //               mCallback.bufferStatus(mEncBuffer.computeTimeSpanUsec());
            }
        }

        /**
         * Saves the encoder output to a .mp4 file.
         * <p>
         * We'll drain the encoder to get any lingering data, but we're not going to shut
         * the encoder down or use other tricks to try to "flush" the encoder.  This may
         * mean we miss the last couple of submitted frames if they're still working their
         * way through.
         * <p>
         * We may want to reset the buffer after this -- if they hit "capture" again right
         * away they'll end up saving video with a gap where we paused to write the file.
         */
        /*
        void saveVideo(File outputFile) {
            if (VERBOSE) Log.d(TAG, "saveVideo " + outputFile);

            int index = mEncBuffer.getFirstIndex();
            if (index < 0) {
                Log.w(TAG, "Unable to get first index");
                mCallback.fileSaveComplete(1);
                return;
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            MediaMuxer muxer = null;
            int result = -1;
            try {
                muxer = new MediaMuxer(outputFile.getPath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int videoTrack = muxer.addTrack(mEncodedFormat);
                muxer.start();

                do {
                    ByteBuffer buf = mEncBuffer.getChunk(index, info);
                    if (VERBOSE) {
                        Log.d(TAG, "SAVE " + index + " flags=0x" + Integer.toHexString(info.flags));
                    }
                    muxer.writeSampleData(videoTrack, buf, info);
                    index = mEncBuffer.getNextIndex(index);
                } while (index >= 0);
                result = 0;
            } catch (IOException ioe) {
                Log.w(TAG, "muxer failed", ioe);
                result = 2;
            } finally {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            }

            if (VERBOSE) {
                Log.d(TAG, "muxer stopped, result=" + result);
            }
            mCallback.fileSaveComplete(result);
        }
*/
        /**
         * Tells the Looper to quit.
         */
        void shutdown() {
            if (VERBOSE) Log.d(TAG, "shutdown");
            
            Looper.myLooper().quit();
        }

        /**
         * Handler for EncoderThread.  Used for messages sent from the UI thread (or whatever
         * is driving the encoder) to the encoder thread.
         * <p>
         * The object is created on the encoder thread.
         */
        private static class EncoderHandler extends Handler {
            public static final int MSG_FRAME_AVAILABLE_SOON = 1;
            public static final int MSG_SAVE_VIDEO = 2;
            public static final int MSG_SHUTDOWN = 3;
            public static final int MSG_FEED_ENCODER = 4;

            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private WeakReference<EncoderThread> mWeakEncoderThread;

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            public EncoderHandler(EncoderThread et) {
                mWeakEncoderThread = new WeakReference<EncoderThread>(et);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (VERBOSE) {
                    Log.v(TAG, "EncoderHandler: what=" + what);
                }

                EncoderThread encoderThread = mWeakEncoderThread.get();
                if (encoderThread == null) {
                    Log.w(TAG, "EncoderHandler.handleMessage: weak ref is null");
                    return;
                }

                switch (what) {
                    case MSG_FRAME_AVAILABLE_SOON:
                        encoderThread.frameAvailableSoon();
                        break;
                    case MSG_SAVE_VIDEO:
  //                      encoderThread.saveVideo((File) msg.obj);
                        break;
                    case MSG_SHUTDOWN:
                        encoderThread.shutdown();
                        break;
                    case MSG_FEED_ENCODER:
                    	boolean eosflag=false;
                    	if(msg.arg1>0)eosflag=true;
                    	int index=msg.arg2;
                    	encoderThread.feedEncoder(eosflag,index);
                    	break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
            }
        }
    }
}
