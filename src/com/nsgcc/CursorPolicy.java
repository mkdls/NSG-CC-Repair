package com.nsgcc;

/** Acquisition can remain live while the displayed cursor is locked to history. */
final class CursorPolicy {
 static long select(boolean testing,boolean locked,long selected,long latest){
  return testing&&!locked?latest:selected;
 }
}
