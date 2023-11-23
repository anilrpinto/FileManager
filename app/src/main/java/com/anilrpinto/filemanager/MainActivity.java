package com.anilrpinto.filemanager;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Map<String, Boolean> map = new HashMap<>();

    private List<String> dirs = new ArrayList<>();

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView lstvSourceDirs = findViewById(R.id.lstvSourceDirs);
        lstvSourceDirs.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        CustomAdapter adapter = new CustomAdapter(this, android.R.layout.simple_list_item_checked, dirs);
        lstvSourceDirs.setAdapter(adapter);

        lstvSourceDirs.setOnItemClickListener((parent, view, position, id) -> {
            map.put(dirs.get(position), ((CheckedTextView) view).isChecked());
            adapter.notifyDataSetChanged();
        });

        findViewById(R.id.btnClear).setOnClickListener(view -> {

            if (map.values().contains(Boolean.valueOf(true))) {

                Iterator<String> keys = map.keySet().iterator();

                while (keys.hasNext()) {
                    String k = keys.next();
                    if (map.get(k)) {
                        dirs.remove(k);
                        keys.remove();
                    }
                }
            } else
                dirs.clear();

            Log.d("", map.toString());
            Log.d("", dirs.toString());

            adapter.notifyDataSetChanged();
        });

        findViewById(R.id.btnBrowse).setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), 100);
        });

        TextView txvFilesCount = findViewById(R.id.txvFilesCount);
        TextView txvFilesSize = findViewById(R.id.txvFilesSize);

        TextView txvDupCount = findViewById(R.id.txvDupCount);
        TextView txvDupSize = findViewById(R.id.txvDupSize);
        TextView  txvDuplicatesHdr = findViewById(R.id.txvDuplicatesHdr);

        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView txvProgress = findViewById(R.id.txvProgress);

        Button btnEmptyDirs = findViewById(R.id.btnEmptyDirs);

        btnEmptyDirs.setOnClickListener(view -> {

            btnEmptyDirs.setText("Processing...");
            txvDuplicatesHdr.setText("Removed");

            executor.execute(() -> {

                try {
                    FileManager.deleteDirs(dirs,  (code, data) -> {
                        runOnUiThread(() -> {
                            switch (code) {

                                case "files-count":
                                    txvFilesCount.setText(String.valueOf(data));
                                    break;

                                case "delete-count":
                                    txvDupCount.setText(String.valueOf(data));
                                    break;

                                case "start":
                                    progressBar.setIndeterminate(true);
                                    break;

                                case "current":
                                    txvProgress.setText((String) data);
                                    Log.d("", (String) data);
                                    break;

                                case "done":
                                    btnEmptyDirs.setText("Remove empty");
                                    txvProgress.setText(null);
                                    progressBar.setIndeterminate(false);
                                    break;

                                default:
                                    Log.d("TAG", "Unhandled event code:" + code);
                            }
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        Button btnFindDuplicates = findViewById(R.id.btnFindDuplicates);

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

                String path = FileUtils.getFullPathFromTreeUri(data.getData(),this);
                if (!dirs.contains(path)) {
                    dirs.add(path);
                    map.put(path, false);
                }

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

        private int origColor = Color.TRANSPARENT;

        public CustomAdapter(@NonNull Context context, int resId, List<String> list) {

            // pass the context and arrayList for the super
            // constructor of the ArrayAdapter class
            super(context, resId, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            CheckedTextView textView = (CheckedTextView) super.getView(position, convertView, parent);
            String data = getItem(position);

            boolean checked = map.get(dirs.get(position));

            Log.d("", "checked:" + checked);

            Log.d("", "checked2:" + textView.isChecked());

            if (checked != textView.isChecked()) {
                textView.setChecked(checked);
                notifyDataSetChanged();
            }


            if (position == 0)
                textView.setBackgroundColor(Color.LTGRAY);
            else {

                if (map.get(data)) {
                    //origColor = ((ColorDrawable) textView.getBackground()).getColor();
                    textView.setBackgroundColor(Color.YELLOW);
                    //textView.setChecked(map.get(data));
                } else
                    textView.setBackgroundColor(Color.RED);
            }
            textView.setText(Utility.truncate(data, 25));
            return textView;
        }
    }

}