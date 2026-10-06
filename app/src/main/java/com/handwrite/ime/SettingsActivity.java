package com.handwrite.ime;
import android.Manifest; import android.content.Intent; import android.content.pm.PackageManager;
import android.database.Cursor; import android.net.Uri; import android.os.*;
import android.provider.OpenableColumns; import android.provider.Settings;
import android.view.View; import android.view.inputmethod.InputMethodManager;
import android.widget.*; import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat;
import java.io.File; import java.util.List;
public class SettingsActivity extends AppCompatActivity {
  private static final int REQ_AU=1002,REQ_PL=2001,REQ_BG=2002;
  private TextView info,plist,blist;
  private PluginManager pm; private BackgroundManager bm;
  @Override protected void onCreate(@Nullable Bundle s){
    super.onCreate(s);
    pm=new PluginManager(this); bm=new BackgroundManager(this);
    ScrollView sc=new ScrollView(this); LinearLayout r=new LinearLayout(this);
    r.setOrientation(LinearLayout.VERTICAL);
    int pad=(int)(getResources().getDisplayMetrics().density*16);
    r.setPadding(pad,pad,pad,pad); sc.addView(r);
    TextView ti=new TextView(this); ti.setText("手写·语音输入法"); ti.setTextSize(22); r.addView(ti);
    info=new TextView(this); info.setTextSize(13); info.setPadding(0,pad,0,pad); r.addView(info);
    btn(r,"① 开启输入法",v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
    btn(r,"② 选择本输入法",v->{
      InputMethodManager im=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
      if(im!=null) im.showInputMethodPicker();
    });
    btn(r,"③ 录音权限（语音必须）",v->{
      if(ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},REQ_AU);
      else toast("已有");
    });
    btn(r,"④ 导入插件 JSON",v->{
      Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*");
      i.addCategory(Intent.CATEGORY_OPENABLE);
      try{ startActivityForResult(i,REQ_PL); }catch(Throwable t){ toast("无法打开"); }
    });
    btn(r,"⑤ 导入背景图片",v->{
      Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*");
      i.addCategory(Intent.CATEGORY_OPENABLE);
      try{ startActivityForResult(i,REQ_BG); }catch(Throwable t){ toast("无法打开"); }
    });
    btn(r,"⑥ 输入测试区",v->startActivity(new Intent(this,TestInputActivity.class)));
    plist=new TextView(this); plist.setTextSize(13); plist.setPadding(0,pad,0,0); r.addView(plist);
    blist=new TextView(this); blist.setTextSize(13); blist.setPadding(0,pad,0,0); r.addView(blist);
    setContentView(sc); refresh();
  }
  private void btn(LinearLayout r,String t,View.OnClickListener l){
    Button b=new Button(this); b.setText(t); b.setAllCaps(false); b.setOnClickListener(l);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin=8; r.addView(b,lp);
  }
  private void refresh(){
    info.setText("剪贴板上限：100000 字符\n手写：ML Kit 中英数字\n语音：系统识别\n背景：内置渐变 + MT2 图标 + 导入图片\n插件：SAF 导入 JSON");
    List<File> ps=pm.list();
    StringBuilder p=new StringBuilder("已导入插件（"+ps.size()+"）：\n");
    if(ps.isEmpty()) p.append("（无）\n"); else for(File f:ps) p.append("• ").append(f.getName()).append("\n");
    plist.setText(p.toString());
    List<String> as=bm.listAssets(); List<File> us=bm.listUser();
    StringBuilder b=new StringBuilder();
    b.append("内置渐变：").append(BackgroundManager.BUILTIN_NAMES.length).append(" 张\n");
    b.append("assets 图片：").append(as.size()).append(" 张\n");
    b.append("已导入图片：").append(us.size()).append(" 张\n");
    blist.setText(b.toString());
  }
  @Override protected void onActivityResult(int req,int res,@Nullable Intent data){
    super.onActivityResult(req,res,data);
    if(res!=RESULT_OK||data==null||data.getData()==null) return;
    Uri u=data.getData(); String n=name(u);
    if(req==REQ_PL){ File f=pm.importUri(u,n); if(f!=null){ toast("插件已导入："+f.getName()); refresh(); } else toast("导入失败"); }
    else if(req==REQ_BG){ File f=bm.importUri(u,n); if(f!=null){ bm.set(BackgroundManager.USER,f.getAbsolutePath()); toast("背景已导入并设为当前"); refresh(); } else toast("导入失败"); }
  }
  private String name(Uri u){
    String n="import_"+System.currentTimeMillis(); Cursor c=null;
    try{ c=getContentResolver().query(u,null,null,null,null);
      if(c!=null&&c.moveToFirst()){ int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if(i>=0){ String x=c.getString(i); if(x!=null&&!x.isEmpty()) n=x; } }
    }catch(Throwable ignored){} finally{ if(c!=null) c.close(); }
    return n;
  }
  @Override protected void onResume(){ super.onResume(); refresh(); }
  private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
}
