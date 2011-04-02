public class Main {
    public static void main(String[] argv) {
        //Argument 0: Jar Location
        //Argument 1: New Name
        System.out.println("Java String Obfuscator");
        JarLoader loader = new JarLoader(argv[0]);
        StringTransformer trans = new StringTransformer(loader);
        trans.transform();
        trans.getJarLoader().save(argv[1]);
        System.out.println("Operation Completed!");
    }
}
