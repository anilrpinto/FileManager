package com.anilrpinto.filemanager;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.anilrpinto.filemanager.utils.FileManager;
import com.anilrpinto.filemanager.utils.FileUtils;
import com.anilrpinto.filemanager.utils.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private List<String> dirs = new ArrayList<>();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lstvSourceDirs = (ListView) findViewById(R.id.lstvSourceDirs);
        lstvSourceDirs.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        lstvSourceDirs.setAdapter(new CustomAdapter(this, android.R.layout.simple_list_item_multiple_choice, dirs));

        findViewById(R.id.btnBrowse).setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), 100);
        });

        TextView txvFilesCount = findViewById(R.id.txvFilesCount);
        TextView txvFilesSize = findViewById(R.id.txvFilesSize);

        TextView txvDupCount = findViewById(R.id.txvDupCount);
        TextView txvDupSize = findViewById(R.id.txvDupSize);

        Button btnFindDuplicates = findViewById(R.id.btnFindDuplicates);

        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView txvProgress = findViewById(R.id.txvProgress);

        btnFindDuplicates.setOnClickListener(view -> {

            btnFindDuplicates.setText("Processing...");

            executor.execute(() -> {
                try {
                    FileManager.sort(dirs, ((CheckBox) findViewById(R.id.chbxIncludeSubFldrs)).isChecked(),
                        ((CheckBox) findViewById(R.id.chbxImagesOnly)).isChecked(), (code, data) -> {

                            runOnUiThread(() -> {
                                switch (code) {

                                    case "files-count":
                                        txvFilesCount.setText(String.valueOf(data));
                                        break;

                                    case "files-size":
                                        txvFilesSize.setText((String) data);
                                        break;

                                    case "dup-count":
                                        txvDupCount.setText(String.valueOf(data));
                                        break;

                                    case "dup-size":
                                        txvDupSize.setText((String) data);
                                        break;

                                    case "start":
                                        progressBar.setMax((int) data);
                                        break;

                                    case "progress":
                                        progressBar.setProgress((int) data);
                                        break;

                                    case "current":
                                        txvProgress.setText((String) data);
                                        break;

                                    case "done":
                                        btnFindDuplicates.setText("Duplicates");
                                        txvProgress.setText(null);
                                        break;

                                    default:
                                        Log.d("TAG", "Unhandled event code:" + code);
                                }
                            });
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 200);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {

            case 100:

                dirs.add(FileUtils.getFullPathFromTreeUri(data.getData(),this));
                ((ArrayAdapter) ((ListView) findViewById(R.id.lstvSourceDirs)).getAdapter()).notifyDataSetChanged();
                break;

            case 200:
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage access granted", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private class CustomAdapter extends ArrayAdapter<String> {

        public CustomAdapter(@NonNull Context context, int resId, List<String> list) {

            // pass the context and arrayList for the super
            // constructor of the ArrayAdapter class
            super(context, resId, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView textView = (TextView) super.getView(position, convertView, parent);
            String data = getItem(position);

            textView.setText(Utility.truncate(data, 25));

            return textView;
        }
    }

}