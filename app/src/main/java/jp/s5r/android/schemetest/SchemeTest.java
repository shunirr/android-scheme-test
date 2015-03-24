package jp.s5r.android.schemetest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;

import jp.s5r.android.schemetest.annotation.SchemeParam;
import jp.s5r.android.schemetest.annotation.SchemeUrl;

public final class SchemeTest {

    public static void handle(Object target, Intent intent) {
        handle(target, intent.getData(), intent.getExtras());
    }

    public static void handle(Object target, Uri uri) {
        handle(target, uri, null);
    }

    private static void handle(Object target, Uri uri, Bundle params) {
        Class clazz = target.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            try {
                Annotation[] annotations = method.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(SchemeUrl.class)) {
                        SchemeUrl schemeUrl = (SchemeUrl) annotation;
                        SchemeHandler handler = new SchemeHandler(schemeUrl.value(), uri, params);
                        if (handler.isMatch()) {
                            Log.d("SchemeTest", "invoke: " + schemeUrl.value());
                            method.invoke(target, getParams(method, handler));
                        }
                    }
                }
            } catch (IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Object[] getParams(Method method, SchemeHandler handler) {
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            return null;
        }

        ArrayList<Object> objectArrayList = new ArrayList<>();

        int i = 0;
        for (Annotation[] annotations : paramAnnotations) {
            Class paramType = paramTypes[i++];
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(SchemeParam.class)) {
                    SchemeParam schemeParam = (SchemeParam) annotation;
                    String key = schemeParam.value();
                    if (paramType.equals(String.class)) {
                        String value = handler.getParamString(key);
                        objectArrayList.add(value);
                    } else if (paramType.equals(int.class) || paramType.equals(Integer.class)) {
                        Integer value = handler.getParamInteger(key);
                        objectArrayList.add(value);
                    }
                }
            }
        }

        if (objectArrayList.size() == 0) {
            return null;
        } else {
            return objectArrayList.toArray();
        }
    }
}
