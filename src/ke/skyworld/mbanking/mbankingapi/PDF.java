package ke.skyworld.mbanking.mbankingapi;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class PDF {
    public static void fnCreate(String theFileName, String theData, String thePassword) throws Exception {
        Method method;
        File file = new File("pdf-builder.jar");
        if (!file.exists()) {
            System.out.println("File Does Not Exist");
            return;
        }
        URL url = file.toURI().toURL();
        URL[] urls = new URL[]{url};
        ClassLoader classLoader = new URLClassLoader(urls);

        Class<?> repoClass = classLoader.loadClass("skyworld.pdfbuilder.Main");
        Object repoClassInstance = repoClass.getDeclaredConstructor().newInstance();

        try {
            method = repoClass.getDeclaredMethod("fnCreatePDFFile", String.class, String.class, String.class, String.class);

            method.invoke(repoClassInstance, theFileName, "", theData, thePassword);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            method = null;
            file = null;
            url = null;
            urls = null;
            classLoader = null;
            repoClass = null;
            repoClassInstance = null;
        }
    }
}
