package com.nsgcc;
import android.app.Activity;import android.os.Bundle;import android.widget.*;
public final class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(40,70,40,40);
  box.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(40,i.getSystemWindowInsetTop()+40,40,i.getSystemWindowInsetBottom()+35);return i;});
  String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  TextView title=new TextView(this);title.setText("NSG CC Repair · "+version);title.setTextSize(24);box.addView(title);
  TextView text=new TextView(this);text.setText("Standalone LSPosed carrier repair module\n\n1. Enable this module in LSPosed and select Network Signal Guru (com.qtrun.QuickTest) as its scope.\n2. Disable the old NSG Export module to prevent both modules from modifying the same carrier list.\n3. Fully restart NSG. For Testing, enable raw log recording and start a new recording. Allow the root read request when prompted.\n4. View the repaired rows in NSA Data Metrics. Use the CC repair status button on Signaling to view and copy diagnostics.\n\nVerified profiles: qtrun 4.8.10, Shannon ICD9, 1700.24.79 and 1700.26.36; n257, 100 MHz, 120 kHz. A frequency marked * is an RRC-derived grid center. Missing values remain unknown.\n\nThis version retains the existing repair logic without a display buffer for brief returns to 3CC. Testing behavior still requires field verification.");text.setTextSize(16);box.addView(text);ScrollView scroll=new ScrollView(this);scroll.addView(box);setContentView(scroll);
 }
}
