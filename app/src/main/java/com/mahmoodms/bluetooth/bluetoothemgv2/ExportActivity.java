package com.mahmoodms.bluetooth.bluetoothemgv2;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ExportActivity extends Activity {

    //2D Array of Floats:
    private Float[][] data_table; // Empty 2D Float Table
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        if(getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if(getActionBar()!=null){
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#303F9F")));
        ArrayList<Float> row1 = (ArrayList<Float>) getIntent().getSerializableExtra("EXTRA_EMG_DATA");
//        ArrayList<Float> row2 = (ArrayList<Float>) getIntent().getSerializableExtra("EXTRA_A_DATA");
//        ArrayList<Float> row3 = (ArrayList<Float>) getIntent().getSerializableExtra("EXTRA_P_DATA");
//        ArrayList<Float> row4 = (ArrayList<Float>) getIntent().getSerializableExtra("EXTRA_R_DATA");
//        compileData(row1,row2,row3,row4);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //return to MainActivity:
                Intent intent = new Intent(this, MainActivity.class);
                /**
                 * Call following flag which will lose all the Activities in the call stack which
                 * are above your main activity and bring your main activity to the top of the
                 * call stack.
                 */
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void compileData(final ArrayList<Float> row1,
                            final ArrayList<Float> row2,
                            final ArrayList<Float> row3,
                            final ArrayList<Float> row4) {
        int row1size = row1.size();
        int row2size = row2.size();
        int row3size = row3.size();
        int row4size = row4.size();
        Log.e("ARRAY SIZES",String.valueOf(row1size)+" "+
                            String.valueOf(row2size)+" "+
                            String.valueOf(row3size)+" "+
                            String.valueOf(row4size));
        int largest = Math.max(row1size,row2size);
        //Initialize data_table:
        data_table = new Float[4][largest];
        //Row1:
        for(int i=0; i<row1size-1; i++){
            data_table[0][i] = (float)row1.get(i);
        }
        //Row2:
        for(int i=0; i<row2size-1; i++){
            data_table[1][i] = (float)row2.get(i);
        }
        //Row3:
        for(int i=0; i<row3size-1; i++){
            data_table[2][i] = (float)row3.get(i);
        }
        //Row4
        for(int i=0; i<row4size-1; i++){
            data_table[3][i] = (float)row4.get(i);
        }
    }

    public void exportFile(View view) throws IOException {
        File file;
        File root = Environment.getExternalStorageDirectory();
        if(root.canWrite()) {
            File dir = new File (root.getAbsolutePath()+"/DataDirectory");
            boolean mkdirsA = dir.mkdirs();
            EditText editText = (EditText) findViewById(R.id.custom_file_name);
            String fileName = editText.getText().toString();
            if (fileName.matches("")) {
                file = new File(dir, "data_untitled.csv");
            } else {
                file = new File(dir, fileName+".csv");
            }
            /*if(!file.isFile()) file.createNewFile();*/
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            int rowCount = data_table.length;

            for (int i = 0; i < rowCount; i++) {
                int columnCount = data_table[i].length;
                String[] values = new String[columnCount];
                for (int j = 0; j < columnCount; j++) {
                    values[j] = data_table[i][j] + "";
                }
                csvWriter.writeNext(values);
            }

            csvWriter.flush();
            csvWriter.close();
            //Export via Email/Dropbox as well:
            Uri uii;
            uii = Uri.fromFile(file);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT,"Data Export Details");
            sendIntent.putExtra(Intent.EXTRA_STREAM, uii);
            sendIntent.setType("text/html");
            startActivity(sendIntent);
        }
    }
}