package com.wardrobe.www;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.wardrobe.www.base.util.BaseUtils;
import com.wardrobe.www.adapter.AlbumAdapter;
import com.wardrobe.www.base.db.DatabaseHelper;
import com.wardrobe.www.base.model.Clothes;
import com.wardrobe.www.service.serviceImpl.ClothesServiceImpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Class Name：UpdatePhotoActivity
 * Class Function（Chinese）：用户可在该界面进入拍照模式，也可以选择本地相册中已存在的图片
 * Class Function（English）：1.Taking photo, 2.Choosing photos which are in the local photo album.
 * Created by Summer on 2016/9/22.
 */

public class AlbumActivity extends BaseActivity {
    private static final String TAG = "AlbumActivity";
    private static final int REQUEST_PERMISSION_CAMERA = 1;
    private static final int LIST_TAKE_PHOTO = 3;
    private static final int TAKE_PHOTO = 4;

    private String photosPath = Environment.getExternalStorageDirectory().getPath() + "/Wardrobe/Photo/";
    private Intent intent;
    private Bundle bundle;
    private Clothes clothes;
    private ClothesServiceImpl clothesService;
    private RecyclerView recyclerView;
    private List<Clothes> clothesList;
    private List<Clothes> selectedClothesList;
    private SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());//设置时间格式
    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());//设置日期格式
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        init();
    }

    private void init() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(this);
        }
        initClothes();
        initIntent();
        initToolbar();
        initRecycler();
    }

    private void initIntent() {
        intent = getIntent();
        if (intent == null) {
            intent = new Intent();
        }
        bundle = intent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
    }

    private void initClothes() {
        if (clothesService == null) {
            clothesService = new ClothesServiceImpl();
        }
        if (clothes == null) {
            clothes = new Clothes();
        }
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_unfold);
        setSupportActionBar(mToolbar);
        android.support.v7.app.ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setTitle("");
            Button leftBtn = (Button) findViewById(R.id.toolbar_unfold_btn_left);
            leftBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_back));
            leftBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlbumActivity.this.finish();
                }
            });
            Button rightBtn = (Button) findViewById(R.id.toolbar_unfold_btn_right);
            rightBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_aff));
            rightBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (Clothes temp : selectedClothesList) {//遍历选中的照片
                        copyFile(temp.getImgUrl(), photosPath + temp.getName());
                        temp.setDivision(bundle.getString("division"));
                        temp.setDate(sdfDate.format(new Date()));
                        clothesService.insertClothes(databaseHelper, temp);

                    }
                    AlbumActivity.this.finish();
                }
            });
            TextView titleText = (TextView) findViewById(R.id.toolbar_unfold_text_title);
            titleText.setText(R.string.choose_photo);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            AlbumActivity.this.finish();
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     */
    public void copyFile(String oldPath, String newPath) {
        File fromFile = new File(oldPath);
        File toFile = new File(newPath);
        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists()) {
            return;
        }
        try {
            BufferedInputStream fosFrom = new BufferedInputStream(new FileInputStream(fromFile)); //读入原文件
            BufferedOutputStream fosTo = new BufferedOutputStream(new FileOutputStream(toFile));
            byte[] bt = new byte[1024];
            int c;
            while ((c = fosFrom.read(bt)) >= 0) {
                fosTo.write(bt, 0, c);
            }
            fosFrom.close();
            fosTo.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void initRecycler() {
        initPhotos();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_album);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));//设置布局管理器
        // 设置item动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new SpaceItemDecoration());
        AlbumAdapter albumAdapter = new AlbumAdapter(clothesList);
        albumAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(albumAdapter);// 为GridView设置适配器
        albumAdapter.openLoadAnimation();
        recyclerView.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void onSimpleItemClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
                if (i == 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        doGetPermission();
                    }
                    doTakePhoto();
                } else {
                    Clothes temp = (Clothes) baseQuickAdapter.getItem(i);
                    if (!temp.isChosen()) {
                        temp.setChosen(true);
                        temp.setTime(sdfTime.format(new Date()));
                        selectedClothesList.add(temp);
                        view.findViewById(R.id.layout_recycler_album).setVisibility(View.VISIBLE);
                    } else {
                        temp.setChosen(false);
                        selectedClothesList.remove(temp);
                        view.findViewById(R.id.layout_recycler_album).setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void doGetPermission() {//申请相机的相关授权
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        }
    }

    private void doTakePhoto() {
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, TAKE_PHOTO);
        File out = new File(getPhotoPath());
        Uri uri = Uri.fromFile(out);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, LIST_TAKE_PHOTO);
    }

    private void initPhotos() {
        ContentResolver mContentResolver = getContentResolver();
        if (selectedClothesList == null) {
            selectedClothesList = new ArrayList<>();
        }
        if (clothesList != null && clothesList.size() > 0) {
            clothesList.clear();
        } else {
            clothesList = new ArrayList<>();
        }
        Cursor mCursor = mContentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.ImageColumns.DATA}, "", null,
                MediaStore.MediaColumns.DATE_ADDED + " DESC");
        if (mCursor != null && mCursor.getCount() > 0) {
            Clothes temp;
            if (mCursor.moveToFirst()) {
                int _date = mCursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    // 获取图片的路径
                    String path = mCursor.getString(_date);
                    String photoName = path.substring(path.lastIndexOf("/") + 1);
                    temp = new Clothes();
                    temp.setImgUrl(path);
                    temp.setName(photoName);
                    clothesList.add(temp);
                    if (clothesList != null && clothesList.size() > 1) {
                        clothesList = BaseUtils.quickSort(clothesList, 1, clothesList.size() - 1);
                    }
                    // 获取该图片的父路径名
//                    File parentFile = new File(path).getParentFile();
//                    if (parentFile == null) {
//                        continue;
//                    }
//                    String dirPath = parentFile.getAbsolutePath();
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "The permission of camera is granted.");
                doTakePhoto();
            } else {
                Log.e(TAG, "the permission of camera is denied.");
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//                    AlertDialog dialog = new AlertDialog.Builder(this)
//                            .setMessage(R.string.camera_do_not_available_hint)
//                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            }).create();
//                    dialog.show();
//                }
                Toast.makeText(AlbumActivity.this, getString(R.string.camera_do_not_available_hint), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == LIST_TAKE_PHOTO) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(clothes.getImgUrl());
                // 防止OOM发生
                options.inJustDecodeBounds = false;
                bitmap.recycle();//回收
                if (clothesService.insertClothes(databaseHelper, clothes) > 0) {
                    this.finish();
                }
            }
        }
    }

    /**
     * 获取所拍摄照片的存储路径
     *
     * @return The path of photo which are photographed just now.
     */
    private String getPhotoPath() {
        // 照片所存放的文件夹路径
        File file = new File(photosPath);
        file.mkdirs();
        //照片的命名，目标文件夹下，以当前时间数字串为名称，即可确保每张照片名称不相同。
        String photoName;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());//获取当前时间，进一步转化为字符串
        photoName = format.format(new Date());
        String photoPath = photosPath + photoName + ".jpg";
        Log.d(TAG, "File : " + photoPath);
        clothes.setName(photoName + ".jpg");
        clothes.setImgUrl(photoPath);
        clothes.setDate(sdfDate.format(new Date()));
        clothes.setTime(sdfTime.format(new Date()));
        clothes.setDivision(bundle.getString("division"));//照片所属的服饰分类
        return photoPath;
    }

    public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        int mSpace;

        SpaceItemDecoration() {
            this.mSpace = AlbumActivity.this.getResources().getDimensionPixelSize(R.dimen.divider);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int pos = parent.getChildAdapterPosition(view);

            if (pos > 2) {
                outRect.top = mSpace / 2;
            } else {
                outRect.top = mSpace;
            }
            if (pos % 3 == 0) {
                outRect.left = mSpace;
            } else if (pos % 3 == 1) {
                outRect.right = mSpace / 2;
                outRect.left = mSpace / 2;
            } else if (pos % 3 == 2) {
                outRect.right = mSpace;
            }

        }
    }

}
