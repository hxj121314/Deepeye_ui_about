package com.buptant.deepeye;

/**
 * Created by thinkpad on 2016/8/9.
 */
public class ModeMsg {
    private String mode;
    private boolean[] select;
    private int modeNum;

    public String getMode(){
        return mode;
    }
    public boolean[] getSelect(){
        return select;
    }
    public int getmodeNum(){
        return modeNum;
    }

    public void setMode(String mode){
        this.mode=mode;
    }
    public void setSelect(boolean[] select){
        this.select=select;
    }
    public void setModeNum(int num){
        this.modeNum=num;
    }
}
