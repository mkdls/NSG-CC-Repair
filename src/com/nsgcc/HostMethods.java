package com.nsgcc;
import java.lang.reflect.*;
import java.util.*;
/** Resolve a renamed host method by its complete signature, never by parameter count. */
public final class HostMethods {
 private HostMethods(){}
 public static Method resolve(Class<?> type,String preferred,Class<?> result,Class<?>... parameters)throws NoSuchMethodException{
  for(Class<?> c=type;c!=null;c=c.getSuperclass())try{
   Method m=c.getDeclaredMethod(preferred,parameters);
   if(m.getReturnType()==result&&!Modifier.isStatic(m.getModifiers())){m.setAccessible(true);return m;}
  }catch(NoSuchMethodException ignored){}
  for(Class<?> c=type;c!=null;c=c.getSuperclass()){
   List<Method> matches=new ArrayList<>();
   for(Method m:c.getDeclaredMethods())if(!m.isBridge()&&!m.isSynthetic()&&!Modifier.isStatic(m.getModifiers())&&m.getReturnType()==result&&Arrays.equals(m.getParameterTypes(),parameters))matches.add(m);
   if(matches.size()>1)throw new NoSuchMethodException("Ambiguous host signature in "+c.getName()+": "+matches);
   if(matches.size()==1){Method m=matches.get(0);m.setAccessible(true);return m;}
  }
  throw new NoSuchMethodException("No host signature "+type.getName()+" "+result.getName()+Arrays.toString(parameters)+"; declared="+Arrays.toString(type.getDeclaredMethods()));
 }
}
