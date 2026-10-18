package com.nsgcc;
import java.lang.reflect.Field;
final class ReflectionFields {
 static Field find(Class<?> type,String name)throws NoSuchFieldException {
  for(Class<?> current=type;current!=null;current=current.getSuperclass()){
   try {Field result=current.getDeclaredField(name);result.setAccessible(true);return result;}
   catch(NoSuchFieldException missing) { /* Continue through inherited fields. */ }
  }
  throw new NoSuchFieldException(type.getName()+" hierarchy: "+name);
 }
}
