package com.example.tensorflow.tensorflowdemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.reactivestreams.Subscription;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult;
    private ImageView imgPic;

    private Classifier classifier;

    //初始化模型的一些参数
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";
    private static final String MODEL_FILE = "file:///android_asset/model/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/model/imagenet_comp_graph_label_strings.txt";

    //权限申请的请求码
    private static final int REQUEST_CODE_TAKE_PICTURE = 1;
    private static final int REQUEST_CODE_OPEN_ALBUM = 2;

    //Loading框
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tv_result);
        imgPic = findViewById(R.id.img_pic);

        progressDialog = new ProgressDialog(this);

        /**
         * 异步初始化TensorFlow
         */
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
                observableEmitter.onNext("processing");
                if (classifier == null) {
                    // 创建 Classifier
                    classifier = TensorFlowImageClassifier.create(MainActivity.this.getAssets(),
                            MODEL_FILE, LABEL_FILE, INPUT_SIZE, IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME);
                }
                observableEmitter.onNext("finish");
                observableEmitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {

                    }

                    @Override
                    public void onNext(String s) {
                        progressDialog.setMessage(s);
                        if (!progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {
                        progressDialog.dismiss();
                    }
                });
    }

    /**
     * 拍照片
     *
     * @param view
     */
    public void onClickTakePhoto(View view) {
        takePhoto(this);
    }

    /**
     * 调用系统相机拍照片
     *
     * @param context
     */
    private void takePhoto(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //还未取得拍照权限
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_TAKE_PICTURE);
        } else {
            //已获得拍照权限
            Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //系统常量， 启动相机的关键
            startActivityForResult(openCameraIntent, REQUEST_CODE_TAKE_PICTURE); // 参数常量为自定义的request code, 在取返回结果时有用
        }
    }

    /**
     * 去相册访问
     *
     * @param view
     */
    public void onClickAlbum(View view) {
        openAlbum(this);
    }

    /**
     * 打开相册
     */
    private void openAlbum(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //还未取得写SD卡权限
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_OPEN_ALBUM);
        } else {
            //已获得写SD卡权限
            Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
            // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型" 所有类型则写 "image/*"
            intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(intentToPickPic, REQUEST_CODE_OPEN_ALBUM);
        }
    }

    /**
     * 权限申请的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
            //拍照权限
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //已申请
                Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //系统常量， 启动相机的关键
                startActivityForResult(openCameraIntent, REQUEST_CODE_TAKE_PICTURE); // 参数常量为自定义的request code, 在取返回结果时有用
            } else {
                Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_OPEN_ALBUM) {
            //打开相册权限
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //已申请
                Intent intentToPickPic = new Intent(Intent.ACTION_PICK, null);
                // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型" 所有类型则写 "image/*"
                intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intentToPickPic, REQUEST_CODE_OPEN_ALBUM);
            } else {
                Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 拍照和相册返回的照片Bitmap处理
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_TAKE_PICTURE:
                if (resultCode == RESULT_OK) {
                    Bitmap bm = (Bitmap) data.getExtras().get("data");
                    analysisBitmap(bm);
                }
                break;
            case REQUEST_CODE_OPEN_ALBUM:
                if (resultCode == RESULT_OK) {
                    //该uri是上一个Activity返回的
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        Bitmap bit = null;
                        try {
                            bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                            if (bit != null) {
                                analysisBitmap(bit);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        Log.i("bit", String.valueOf(bit));
                    }
                }
                break;
        }
    }

    /**
     * 通过模型分析得出结果
     *
     * @param bm
     */
    private void analysisBitmap(final Bitmap bm) {
        imgPic.setImageBitmap(bm);

        Observable.create(new ObservableOnSubscribe<List<Recognition>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Recognition>> observableEmitter) throws Exception {
                try {
                    Bitmap croppedBitmap = getScaleBitmap(bm, INPUT_SIZE);
                    List<Recognition> recognitionList = classifier.recognizeImage(croppedBitmap);
                    observableEmitter.onNext(recognitionList);
                } catch (Exception e) {
                    observableEmitter.onError(e);
                }
                observableEmitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Recognition>>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        progressDialog.setMessage("start");
                        if (!progressDialog.isShowing()) {
                            progressDialog.show();
                        }
                    }

                    @Override
                    public void onNext(List<Recognition> s) {
                        String resultStr = "";
                        for (int i = 0; i < s.size(); i++) {
                            resultStr += s.get(i).toString() +"\n";
                        }
                        tvResult.setText(resultStr);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        progressDialog.dismiss();
                        tvResult.setText(throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        progressDialog.dismiss();
                    }
                });
    }

    /**
     * 对图片进行缩放
     *
     * @param bitmap
     * @param size
     * @return
     * @throws IOException
     */
    private static Bitmap getScaleBitmap(Bitmap bitmap, int size) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) size) / width;
        float scaleHeight = ((float) size) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
