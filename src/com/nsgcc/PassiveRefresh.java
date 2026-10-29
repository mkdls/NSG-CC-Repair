package com.nsgcc;

/** Only module-originated passive updates suppress the signaling page's seek-to-cursor branch. */
final class PassiveRefresh {
 static final Object ORIGIN=new Object();
 private PassiveRefresh(){}
 static Object liveOrigin(boolean locked){return locked?ORIGIN:null;}
 static Object signalingOrigin(Object incoming,Object fragment){return incoming==ORIGIN?fragment:incoming;}
}
