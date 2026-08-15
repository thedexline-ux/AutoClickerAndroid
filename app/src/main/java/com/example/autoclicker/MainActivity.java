package com.example.autoclicker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.View;

public class MainActivity extends Activity {
    private EditText intervalInput, countInput; private TextView status, summary;
    private LinearLayout setup, points, profiles; private int profile=0;
    private final float[] xs={500,500,500,500}, ys={500,500,500,500};
    private final long[] intervals={500,500,500,500}, counts={0,0,0,0};
    private final RectF[] regions={null,null,null,null};
    private SharedPreferences prefs;
    @Override protected void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main); prefs=getSharedPreferences("profiles",0); load(); bind(); showProfile(); }
    private void bind(){
        status=findViewById(R.id.statusText); summary=findViewById(R.id.pointSummary); intervalInput=findViewById(R.id.intervalInput); countInput=findViewById(R.id.countInput);
        setup=findViewById(R.id.setupPanel); points=findViewById(R.id.pointsPanel); profiles=findViewById(R.id.profilesPanel);
        findViewById(R.id.tabSetup).setOnClickListener(v->tab(0)); findViewById(R.id.tabPoints).setOnClickListener(v->tab(1)); findViewById(R.id.tabProfiles).setOnClickListener(v->tab(2));
        findViewById(R.id.accessibilityButton).setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.floatingButton).setOnClickListener(v->{ AutoClickService s=AutoClickService.getInstance(); if(s==null){toast("اول Accessibility را فعال کن");return;} s.showFloatingControls(); toast("کنترل شناور فعال شد"); });
        findViewById(R.id.pickPointButton).setOnClickListener(v->{ saveSelectionToProfile(); save(); AutoClickService s=AutoClickService.getInstance(); if(s==null){toast("اول Accessibility را فعال کن");return;} s.beginPick(1); });
        findViewById(R.id.pickRegionButton).setOnClickListener(v->{ saveSelectionToProfile(); save(); AutoClickService s=AutoClickService.getInstance(); if(s==null){toast("اول Accessibility را فعال کن");return;} s.beginPick(2); });
        findViewById(R.id.startButton).setOnClickListener(v->start()); findViewById(R.id.stopButton).setOnClickListener(v->stop());
        for(int i=0;i<4;i++){ final int p=i; findViewById(getResources().getIdentifier("profile"+(i+1),"id",getPackageName())).setOnClickListener(v->{saveSelectionToProfile(); save(); profile=p; applyProfileSelection(); showProfile(); tab(0);}); }
    }
    private void tab(int t){ setup.setVisibility(t==0?View.VISIBLE:View.GONE); points.setVisibility(t==1?View.VISIBLE:View.GONE); profiles.setVisibility(t==2?View.VISIBLE:View.GONE); findViewById(R.id.tabSetup).setSelected(t==0); findViewById(R.id.tabPoints).setSelected(t==1); findViewById(R.id.tabProfiles).setSelected(t==2); if(t==1) updateSummary(); }
    private void start(){ saveSelectionToProfile(); save(); AutoClickService s=AutoClickService.getInstance(); if(s==null){toast("Accessibility فعال نیست");return;} s.configureSettings(intervals[profile],counts[profile]); s.startClicking(); status.setText("● در حال اجرا"); }
    private void stop(){ AutoClickService s=AutoClickService.getInstance(); if(s!=null)s.stopClicking(); status.setText("○ متوقف"); }
    private void showProfile(){ intervalInput.setText(String.valueOf(intervals[profile])); countInput.setText(String.valueOf(counts[profile])); applyProfileSelection(); updateSummary(); }
    private void save(){ try{intervals[profile]=Math.max(50,Long.parseLong(intervalInput.getText().toString()));counts[profile]=Math.max(0,Long.parseLong(countInput.getText().toString()));}catch(Exception e){toast("مقدار سرعت یا تعداد صحیح نیست");return;} AutoClickService s=AutoClickService.getInstance(); if(s!=null){ /* selection values are kept in service until next explicit pick */ } prefs.edit().putLong("i"+profile,intervals[profile]).putLong("c"+profile,counts[profile]).apply(); }
    private void load(){ for(int i=0;i<4;i++){intervals[i]=prefs.getLong("i"+i,500);counts[i]=prefs.getLong("c"+i,0); float px=prefs.getFloat("x"+i,500), py=prefs.getFloat("y"+i,500); xs[i]=px; ys[i]=py; if(prefs.getBoolean("hasR"+i,false)) regions[i]=new RectF(prefs.getFloat("l"+i,px),prefs.getFloat("t"+i,py),prefs.getFloat("r"+i,px),prefs.getFloat("b"+i,py));} }
    private void saveSelectionToProfile(){ AutoClickService s=AutoClickService.getInstance(); if(s==null)return; RectF r=s.getSelectedRegion(); if(r!=null){ regions[profile]=r; xs[profile]=r.centerX(); ys[profile]=r.centerY(); } else { regions[profile]=null; xs[profile]=s.getSelectedX(); ys[profile]=s.getSelectedY(); } SharedPreferences.Editor e=prefs.edit().putFloat("x"+profile,xs[profile]).putFloat("y"+profile,ys[profile]).putBoolean("hasR"+profile,regions[profile]!=null); if(regions[profile]!=null)e.putFloat("l"+profile,regions[profile].left).putFloat("t"+profile,regions[profile].top).putFloat("r"+profile,regions[profile].right).putFloat("b"+profile,regions[profile].bottom); e.apply(); }
    private void applyProfileSelection(){ AutoClickService s=AutoClickService.getInstance(); if(s==null)return; if(regions[profile]!=null)s.setRegion(regions[profile]); else s.setPoint(xs[profile],ys[profile]); }
    private void updateSummary(){ if(summary==null)return; AutoClickService s=AutoClickService.getInstance(); if(s!=null){ RectF r=s.getSelectedRegion(); if(r!=null){ regions[profile]=r; summary.setText("ناحیه فعال: "+(int)r.width()+" × "+(int)r.height()+"\nمرکز: "+(int)r.centerX()+", "+(int)r.centerY()); return; } xs[profile]=s.getSelectedX(); ys[profile]=s.getSelectedY(); } if(regions[profile]!=null) summary.setText("ناحیه فعال: "+(int)regions[profile].width()+" × "+(int)regions[profile].height()+"\nمرکز: "+(int)regions[profile].centerX()+", "+(int)regions[profile].centerY()); else summary.setText("نقطه فعال: X="+(int)xs[profile]+"  Y="+(int)ys[profile]+"\nمی‌توانی با «انتخاب نقطه» آن را عوض کنی."); }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onResume(){super.onResume(); AutoClickService s=AutoClickService.getInstance(); if(s!=null){status.setText(s.isRunning()?"● در حال اجرا":"○ آماده"); if(s.isRunning())s.showFloatingControls(); updateSummary();} }
    @Override protected void onPause(){ saveSelectionToProfile(); save(); super.onPause(); }
}
