import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.generic.ClassGen;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarLoader {

    public final Map<String, ClassGen> ClassEntries = new HashMap<String, ClassGen>();
    public final Map<String, byte[]> NonClassEntries = new HashMap<String, byte[]>();

    public JarLoader(String fileLocation) {
        try {
            File file = new File(fileLocation);
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry == null) {
                    continue;
                }

                InputStream entryStream = jarFile.getInputStream(entry);
                if (entry.getName().endsWith(".class")) {
                    JavaClass jc = new ClassParser(entryStream, entry.getName()).parse();
                    ClassGen cg = new ClassGen(jc);
                    ClassEntries.put(cg.getClassName(), cg);
                } else {
                    byte[] contents = getBytes(entryStream);
                    NonClassEntries.put(entry.getName(), contents);
                }
            }
        } catch (Exception e) {
            System.out.println("Error Loading Jar! Location: " + fileLocation);
            e.printStackTrace();
        }
    }

    public void save(String fileName) {
        try {
            FileOutputStream os = new FileOutputStream(fileName);
            JarOutputStream jos = new JarOutputStream(os);
            for (ClassGen classIt : ClassEntries.values()) {
                jos.putNextEntry(new JarEntry(classIt.getClassName().replace('.', File.separatorChar) + ".class"));
                jos.write(classIt.getJavaClass().getBytes());
                jos.closeEntry();
                jos.flush();
            }
            for (String n : NonClassEntries.keySet()) {
                JarEntry destEntry = new JarEntry(n);
                byte[] bite = NonClassEntries.get(n);
                if (bite != null) {
                    jos.putNextEntry(destEntry);
                    jos.write(bite);
                    jos.closeEntry();
                }
            }
            jos.closeEntry();
            jos.close();
        } catch (Exception e) {
            System.out.println("Error Saving Jar! Location: " + fileName);
            e.printStackTrace();
        }
    }

    public static byte[] getBytes(InputStream inputStream) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int len = 0;
            if (len < 0) {
                break;
            }
            try {
                len = inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (len < 0) {
                break;
            }
            bout.write(buffer, 0, len);
        }
        return bout.toByteArray();
    }
}
