import com.sun.org.apache.bcel.internal.Constants;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;

public class StringTransformer {
    private JarLoader jarLoader;

    public StringTransformer(JarLoader j) {
        jarLoader = j;
    }

    public void transform() {
        for (ClassGen cg : jarLoader.ClassEntries.values()) {

            MethodGen cipher = getDecryptor(cg, 9001); //Generate method code
            cg.addMethod(cipher.getMethod()); //Add the code and method into the class file
            int index = cg.getConstantPool().addMethodref(cipher); //Add information on the method to the constant pool
            //Constant pool is like a phonebook for other methods to access
            //It contains info on how to find data in a class file

            for (Method m : cg.getMethods()) {
                MethodGen mg = new MethodGen(m, cg.getClassName(), cg.getConstantPool());
                InstructionList list = mg.getInstructionList();
                if (list.isEmpty()) continue;
                InstructionHandle[] handles = list.getInstructionHandles();

                for (InstructionHandle handle : handles) {
                    if (handle.getInstruction() instanceof LDC) {
                        String before = ((LDC) handle.getInstruction()).getValue(cg.getConstantPool()).toString();
                        String after = getEncrypted(before, 9001);
                        System.out.println(cg.getClassName() + "." + mg.getName() + ": " + before + " -> " + after);

                        Instruction ins = new INVOKESTATIC(index); //Create a new INVOKESTATIC instruction that looks at index for information on which method to call
                        list.append(handle, ins); //Append the new invokestatic instruction to the LDC handle.

                        int newIndex = cg.getConstantPool().addString(after);
                        LDC newIns = new LDC(newIndex); //Make a new LDC instruction that points to the new value in the constant pool
                        handle.setInstruction(newIns); //Replace the old LDC with a new one
                        //LDC "data"
                        //InvokeStatic System.out.println(String)
                    }
                }
                list.setPositions(); //Update line tables and whatnot for our modified instruction list
                mg.setInstructionList(list); //Change our temporary methods instruction list to our modified one
                cg.replaceMethod(m, mg.getMethod()); //Replace original method with our new modified one
            }
        }
    }

    private String getEncrypted(String input, int key) {
        char[] inputChars = input.toCharArray();
        for (int i = 0; i < inputChars.length; i++) {
            inputChars[i] = (char) (inputChars[i] ^ key);
        }
        return new String(inputChars);
    }

    //Generates a cipher method in bytecode for injection into the class
    private MethodGen getDecryptor(ClassGen cg, int key) {
        InstructionList il = new InstructionList();
        InstructionFactory fa = new InstructionFactory(cg);
        il.append(new ALOAD(0));
        il.append(fa.createInvoke("java.lang.String", "toCharArray", new ArrayType(Type.CHAR, 1), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(new ASTORE(2));
        il.append(new ICONST(0));
        il.append(new ISTORE(3));
        il.append(new ILOAD(3));
        il.append(new ALOAD(2));
        il.append(new ARRAYLENGTH());
        il.append(new IF_ICMPGE(il.getInstructionHandles()[0]));//Placeholder, to be replaced
        il.append(new ALOAD(2));
        il.append(new ILOAD(3));
        il.append(new ALOAD(2));
        il.append(new ILOAD(3));
        il.append(new CALOAD());
        il.append(new BIPUSH((byte) key));
        il.append(new IXOR());
        il.append(new I2C());
        il.append(new CASTORE());
        il.append(new IINC(3, 1));
        il.append(new GOTO(il.getInstructionHandles()[5]));
        il.append(fa.createNew(ObjectType.STRING));
        il.append(new DUP());
        il.append(new ALOAD(2));
        il.append(fa.createInvoke("java.lang.String", "<init>", Type.VOID, new Type[]{new ArrayType(Type.CHAR, 1)}, Constants.INVOKESPECIAL));
        il.append(new ARETURN());
        il.getInstructionHandles()[8].setInstruction(new IF_ICMPGE(il.getInstructionHandles()[20]));
        il.setPositions();

        MethodGen mg = new MethodGen(
                Constants.ACC_STATIC | Constants.ACC_PUBLIC, //Access modifiers.
                Type.STRING, //Return Type
                new Type[]{Type.STRING}, //Arguments
                new String[]{"hax"}, //Argument Identifiers
                "pizza" + System.currentTimeMillis(), //Method Name
                cg.getClassName(), //Class Name
                il, //InstructionList for Method
                cg.getConstantPool()); //ConstantPool for Method
        mg.setMaxLocals();
        mg.setMaxStack();
        return mg;
    }

    public JarLoader getJarLoader() {
        return jarLoader;
    }
}
