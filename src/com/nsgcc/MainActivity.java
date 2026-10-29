package com.nsgcc;
import android.app.Activity;import android.os.Bundle;import android.widget.*;
public final class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(40,70,40,40);
  box.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(40,i.getSystemWindowInsetTop()+40,40,i.getSystemWindowInsetBottom()+35);return i;});
  String version="";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  TextView title=new TextView(this);title.setText("NSG CC Repair · "+version);title.setTextSize(24);box.addView(title);
  TextView text=new TextView(this);text.setText("Standalone LSPosed carrier repair module\n\n1. Enable this module in LSPosed and select Network Signal Guru (com.qtrun.QuickTest) as its scope.\n2. Disable combined NSG Export 0.1.35 or earlier. Export 0.1.36 or later can remain enabled.\n3. Fully restart NSG. For Testing, enable raw log recording and start a new recording. Allow the root read request when prompted.\n4. View the repaired rows in NSA Data Metrics. The NR CC Metrics swipe page is provided by NSG Signaling Export. Use CC repair status on Signaling for diagnostics.\n\nVerified profiles: qtrun 4.8.10, Shannon ICD9, 1700.24.79 and 1700.26.36; n257, 100 MHz, 120 kHz. A frequency marked * is an RRC-derived grid center. Missing values remain unknown.\n\nPassive CC Repair updates preserve the Signaling scroll position while the history cursor is locked. Manual time-axis seeks and unlocked live following retain native behavior. The existing carrier repair logic and brief returns to 3CC are unchanged. Handset behavior still needs field verification.");text.setTextSize(16);box.addView(text);ScrollView scroll=new ScrollView(this);scroll.addView(box);setContentView(scroll);
 }
}
