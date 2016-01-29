package ga.softcopy.scramble;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileBrowser extends ListActivity {
    private String path;
    public static String filename, name, scrName;
    TextView textView;
    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        textView = (TextView) findViewById(R.id.title);
        lv = getListView();

        // Use the current directory as title
        path = Environment.getExternalStorageDirectory() + "";
        if (getIntent().hasExtra("path")) {
            path = getIntent().getStringExtra("path");
        }
        //setTitle(path);
        textView.setText(path);

        // Read all files sorted into the values-array
        List values = new ArrayList();
        File dir = new File(path);
        if (!dir.canRead()) {
            Toast.makeText(this, "inaccessible", Toast.LENGTH_SHORT).show();
        }
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                if (!file.startsWith(".")) {
                    values.add(file);
                }
            }
        } else {
            path = Environment.getExternalStorageDirectory()
                    + "/download";
            Intent intent = new Intent(this, FileBrowser.class);
            intent.putExtra("path", path);
            startActivity(intent);
            finish();
        }
        Collections.sort(values);
        // Put the data into the list
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        setListAdapter(adapter);

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
                name = (String) getListAdapter().getItem(position);
                scramble(name);
                return true;
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        filename = (String) getListAdapter().getItem(position);
        name = filename;
        if (path.endsWith(File.separator)) {
            filename = path + filename;
        } else {
            filename = path + File.separator + filename;
        }
        if (new File(filename).isDirectory()) {
            Intent intent = new Intent(this, FileBrowser.class);
            intent.putExtra("path", filename);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (filename.endsWith(".sam")) {
                scrName = filename.substring(0, filename.lastIndexOf('.'));
                openFile(scrName);
            } else {
                openFile(filename);
            }
        }
    }

    void openFile(String str) {
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        try {
            String mimeType = myMime.getMimeTypeFromExtension(fileExt(str).substring(1));
            newIntent.setDataAndType(Uri.fromFile(new File(filename)), mimeType);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_SHORT).show();
        } catch (NullPointerException n) {
            Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_SHORT).show();
        }
    }

    private String fileExt(String url) {
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf("."));
            if (ext.contains("%")) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.contains("/")) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

    private void scramble(String str) {
        if (str.endsWith(".sam")) {
            File from = new File(path, str);
            str = str.substring(0, str.lastIndexOf('.'));
            File to = new File(path, str);
            from.renameTo(to);
            Toast.makeText(this, "unScrambled", Toast.LENGTH_SHORT).show();
            back(path);
        } else {
            Toast.makeText(this, "Scrambled", Toast.LENGTH_SHORT).show();
            File from = new File(path, str);
            File to = new File(path, str + ".sam");
            from.renameTo(to);
            back(path);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void back(String str) {
        Intent intent = new Intent(this, FileBrowser.class);
        intent.putExtra("path", str);
        startActivity(intent);
        finish();
    }

}