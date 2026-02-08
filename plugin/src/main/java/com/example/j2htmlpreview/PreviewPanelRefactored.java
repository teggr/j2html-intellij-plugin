package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.tools.*;
import java.awt.BorderLayout;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Refactored PreviewPanel that uses modular UI components with event-driven architecture.
 * This panel orchestrates the following components:
 * - PreviewState: Shared state model
 * - MethodSelectorPanel: Method selection UI
 * - ExpressionEditorPanel: Expression editing UI
 * - HtmlRendererPanel: HTML rendering UI
 */
public class PreviewPanelRefactored extends JPanel implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(PreviewPanelRefactored.class);
    private static final int DEBOUNCE_DELAY_MS = 400;
    private static final long COMPILATION_THROTTLE_MS = 2500;
    
    private final Project project;
    private final PreviewState state;
    private final Alarm psiChangeAlarm;
    private long lastCompilationTime = 0;
    
    // UI Components
    private final MethodSelectorPanel methodSelectorPanel;
    private final ExpressionEditorPanel expressionEditorPanel;
    private final HtmlRendererPanel htmlRendererPanel;
    
    // Current state
    private VirtualFile currentVirtualFile = null;
    private final List<PsiMethod> j2htmlMethods = new ArrayList<>();
    
    public PreviewPanelRefactored(Project project) {
        this.project = project;
        this.state = new PreviewState();
        this.psiChangeAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        
        setLayout(new BorderLayout());
        
        // Create UI components
        methodSelectorPanel = new MethodSelectorPanel(project);
        expressionEditorPanel = new ExpressionEditorPanel(project);
        htmlRendererPanel = new HtmlRendererPanel(project);
        
        // Register child components as disposables
        Disposer.register(this, methodSelectorPanel);
        Disposer.register(this, expressionEditorPanel);
        Disposer.register(this, htmlRendererPanel);

        // Assemble layout
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(methodSelectorPanel);
        topPanel.add(expressionEditorPanel);
        
        add(topPanel, BorderLayout.NORTH);
        add(htmlRendererPanel, BorderLayout.CENTER);
        
        // Set up listeners
        setupFileListener();
        setupPsiListener();
        setupExecutionListener();
        setupMethodSelectionListener();
        
        // Show current file if one is already open
        updateCurrentFile();
    }
    
    private void setupFileListener() {
        project.getMessageBus()
            .connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                @Override
                public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                    updateCurrentFile();
                }
            });
    }
    
    private void setupPsiListener() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            new PsiTreeChangeAdapter() {
                @Override
                public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
                    PsiFile changedFile = event.getFile();
                    if (changedFile != null && currentVirtualFile != null) {
                        VirtualFile changedVirtualFile = changedFile.getVirtualFile();
                        if (currentVirtualFile.equals(changedVirtualFile)) {
                            psiChangeAlarm.cancelAllRequests();
                            psiChangeAlarm.addRequest(() -> analyzeFile(currentVirtualFile), DEBOUNCE_DELAY_MS);
                        }
                    }
                }
            },
            this
        );
    }
    
    private void setupExecutionListener() {
        project.getMessageBus().connect(this).subscribe(PreviewExecutor.TOPIC, 
            new PreviewExecutor.PreviewExecutorListener() {
                @Override
                public void onExecuteMethod(PsiMethod method) {
                    compileAndExecute(method);
                }
                
                @Override
                public void onExecuteExpression(PsiMethod contextMethod, String expression) {
                    compileAndExecuteExpression(contextMethod, expression);
                }
            });
    }
    
    private void setupMethodSelectionListener() {
        project.getMessageBus().connect(this).subscribe(PreviewState.TOPIC,
            new PreviewState.PreviewStateListener() {
                @Override
                public void onMethodSelected(PsiMethod method) {
                    PsiParameterList parameterList = method != null ? method.getParameterList() : null;
                    if (method != null && parameterList != null && parameterList.getParametersCount() == 0) {
                        // Zero parameters - execute immediately via message bus
                        project.getMessageBus()
                            .syncPublisher(PreviewExecutor.TOPIC)
                            .onExecuteMethod(method);
                    }
                }
            });
    }
    
    private void updateCurrentFile() {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile selectedFile = editorManager.getSelectedFiles().length > 0 
            ? editorManager.getSelectedFiles()[0] 
            : null;
        
        if (selectedFile != null) {
            currentVirtualFile = selectedFile;
            state.setCurrentFile(selectedFile);
            publishFileChanged(selectedFile);
            analyzeFile(selectedFile);
        } else {
            currentVirtualFile = null;
            j2htmlMethods.clear();
            state.setJ2htmlMethods(j2htmlMethods);
            publishMethodsChanged(j2htmlMethods);
            publishHtmlChanged(HtmlTemplates.getInitialPage());
        }
    }
    
    private void analyzeFile(VirtualFile virtualFile) {
        j2htmlMethods.clear();
        
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        
        if (!(psiFile instanceof PsiJavaFile)) {
            publishHtmlChanged(HtmlTemplates.getNotJavaFilePage());
            state.setJ2htmlMethods(j2htmlMethods);
            publishMethodsChanged(j2htmlMethods);
            return;
        }
        
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = javaFile.getClasses();
        
        for (PsiClass psiClass : classes) {
            PsiMethod[] methods = psiClass.getMethods();
            for (PsiMethod method : methods) {
                if (isJ2HtmlMethod(method)) {
                    j2htmlMethods.add(method);
                }
            }
        }
        
        state.setJ2htmlMethods(j2htmlMethods);
        publishMethodsChanged(j2htmlMethods);
        
        if (j2htmlMethods.isEmpty()) {
            publishHtmlChanged(HtmlTemplates.getNoMethodsFoundPage());
        }
    }
    
    private boolean isJ2HtmlMethod(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return false;
        }
        
        String returnTypeName = returnType.getPresentableText();

        // Check for common j2html return types
        if (returnTypeName.equals("ContainerTag") ||
            returnTypeName.equals("DomContent") ||
            returnTypeName.equals("Tag") ||
            returnTypeName.equals("UnescapedText") ||
            returnTypeName.equals("Text")) {
            return true;
        }

        // Check if return type ends with "Tag" (covers DivTag, PTag, FormTag, etc.)
        if (returnTypeName.endsWith("Tag")) {
            return true;
        }

        return false;
    }
    
    private void compileAndExecute(PsiMethod psiMethod) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCompilationTime < COMPILATION_THROTTLE_MS) {
            publishInfo("Please wait... compilation is still in progress or too soon since last compilation.");
            return;
        }
        lastCompilationTime = currentTime;
        
        ReadAction.nonBlocking(() -> ModuleUtilCore.findModuleForPsiElement(psiMethod))
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), module -> {
                if (module == null) {
                    publishError("Could not find module for class");
                    return;
                }
                
                publishInfo("Compiling...");
                
                CompilerManager.getInstance(project).make(module, new CompileStatusNotification() {
                    @Override
                    public void finished(boolean aborted, int errors, int warnings, CompileContext context) {
                        SwingUtilities.invokeLater(() -> {
                            if (aborted) {
                                publishError("Compilation was aborted.");
                            } else if (errors > 0) {
                                publishError("Compilation failed with " + errors + " error(s). Check the Problems panel for details.");
                            } else {
                                executeMethod(psiMethod);
                            }
                        });
                    }
                });
            }).submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }
    
    private void executeMethod(PsiMethod psiMethod) {
        ReadAction.nonBlocking(() -> {
            try {
                PsiClass psiClass = psiMethod.getContainingClass();
                if (psiClass == null) {
                    return new ExecutionData("Could not find containing class for method", null, null, null);
                }
                
                String qualifiedClassName = psiClass.getQualifiedName();
                if (qualifiedClassName == null) {
                    return new ExecutionData("Could not determine qualified class name", null, null, null);
                }
                
                if (!psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
                    return new ExecutionData("Method must be static. Non-static method execution not yet supported.", null, null, null);
                }
                
                if (psiMethod.getParameterList().getParametersCount() > 0) {
                    return new ExecutionData("Method has parameters. Use the expression editor above to provide arguments.", null, null, null);
                }
                
                Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
                if (module == null) {
                    return new ExecutionData("Could not find module for class", null, null, null);
                }
                
                return new ExecutionData(null, qualifiedClassName, psiMethod.getName(), module);
            } catch (Exception e) {
                return new ExecutionData("Error preparing execution: " + e.getMessage(), null, null, null);
            }
        }).finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), data -> {
            if (data.error != null) {
                publishError(data.error);
                return;
            }
            executeMethodOnEdt(data);
        }).submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }
    
    private static class ExecutionData {
        final String error;
        final String qualifiedClassName;
        final String methodName;
        final Module module;
        
        ExecutionData(String error, String qualifiedClassName, String methodName, Module module) {
            this.error = error;
            this.qualifiedClassName = qualifiedClassName;
            this.methodName = methodName;
            this.module = module;
        }
    }
    
    private void executeMethodOnEdt(ExecutionData data) {
        try {
            ClassLoader moduleClassLoader = getModuleClassLoader(data.module);
            
            Class<?> loadedClass;
            try {
                loadedClass = Class.forName(data.qualifiedClassName, true, moduleClassLoader);
            } catch (ClassNotFoundException e) {
                publishError("Class not found. Make sure the project is compiled: " + e.getMessage());
                return;
            }
            
            Method reflectionMethod;
            try {
                reflectionMethod = loadedClass.getDeclaredMethod(data.methodName);
            } catch (NoSuchMethodException e) {
                publishError("Method not found in compiled class: " + e.getMessage());
                return;
            }
            
            reflectionMethod.setAccessible(true);
            
            Object result;
            try {
                result = reflectionMethod.invoke(null);
            } catch (Exception e) {
                publishError("Error invoking method: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                return;
            }
            
            if (result == null) {
                publishError("Method returned null");
                return;
            }
            
            String renderedHtml = renderJ2HtmlObject(result);
            publishHtmlChanged(renderedHtml);
            publishSuccess("Successfully rendered HTML");
            
        } catch (Exception e) {
            publishError("Unexpected error: " + e.getMessage());
            LOG.error("Unexpected error executing method", e);
        }
    }
    
    private ClassLoader getModuleClassLoader(Module module) throws Exception {
        List<String> classpathEntries = new ArrayList<>();
        
        VirtualFile[] roots = OrderEnumerator.orderEntries(module)
          .withoutSdk()
          .recursively()
          .classes()
          .getRoots();
           
        Stream.of(roots).forEach(root -> {
            String path = root.getPath();
            if (path.startsWith("jar://")) {
                path = path.substring(6);
                int exclamation = path.indexOf("!");
                if (exclamation != -1) {
                    path = path.substring(0, exclamation);
                }
            }
            classpathEntries.add(path);
        });
        
        URL[] urls = classpathEntries.stream()
            .map(path -> {
                try {
                    return new File(path).toURI().toURL();
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toArray(URL[]::new);
        
        return new URLClassLoader(urls, getClass().getClassLoader());
    }
    
    private String renderJ2HtmlObject(Object j2htmlObject) throws Exception {
        Method renderMethod = j2htmlObject.getClass().getMethod("render");
        Object result = renderMethod.invoke(j2htmlObject);
        
        if (!(result instanceof String)) {
            throw new Exception("render() did not return a String");
        }
        
        return (String) result;
    }
    
    // Message bus publishing methods
    
    private void publishFileChanged(VirtualFile file) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onFileChanged(file);
    }
    
    private void publishMethodsChanged(List<PsiMethod> methods) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onMethodsChanged(methods);
    }
    
    private void publishHtmlChanged(String html) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onHtmlChanged(html);
    }
    
    private void publishError(String message) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onStatusChanged(PreviewState.PreviewStatus.ERROR, message);
    }
    
    private void publishInfo(String message) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onStatusChanged(PreviewState.PreviewStatus.IDLE, message);
    }
    
    private void publishSuccess(String message) {
        project.getMessageBus()
            .syncPublisher(PreviewState.TOPIC)
            .onStatusChanged(PreviewState.PreviewStatus.SUCCESS, message);
    }
    
    // Expression execution methods

    private void compileAndExecuteExpression(PsiMethod contextMethod, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            publishError("Expression is empty");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCompilationTime < COMPILATION_THROTTLE_MS) {
            publishInfo("Please wait... compilation is still in progress or too soon since last compilation.");
            return;
        }
        lastCompilationTime = currentTime;

        ReadAction.nonBlocking(() -> {
            try {
                JavaCodeFragmentFactory fragmentFactory = JavaCodeFragmentFactory.getInstance(project);
                PsiExpressionCodeFragment fragment = fragmentFactory.createExpressionCodeFragment(
                    expression.trim(),
                    contextMethod.getContainingClass(),
                    contextMethod.getReturnType(),
                    true
                );

                Module module = ModuleUtilCore.findModuleForPsiElement(contextMethod);
                if (module == null) {
                    return new ExpressionExecutionData("Could not find module", null, null);
                }

                return new ExpressionExecutionData(null, fragment, module);
            } catch (Exception e) {
                return new ExpressionExecutionData("Error preparing expression: " + e.getMessage(), null, null);
            }
        }).finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), data -> {
            if (data.error != null) {
                publishError(data.error);
                return;
            }

            publishInfo("Compiling expression...");

            CompilerManager.getInstance(project).make(data.module, new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings, @NotNull CompileContext context) {
                    if (aborted) {
                        publishError("Compilation was aborted.");
                    } else if (errors > 0) {
                        publishError("Compilation failed with " + errors + " error(s). Check the Problems panel.");
                    } else {
                        // Move heavy work to background thread
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            try {
                                evaluateAndDisplay(data.fragment, data.module);
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(() -> {
                                    publishError("Error evaluating expression: " + e.getMessage());
                                    LOG.error("Error evaluating expression", e);
                                });
                            }
                        });
                    }
                }
            });
        }).submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }

    private static class ExpressionExecutionData {
        final String error;
        final PsiExpressionCodeFragment fragment;
        final Module module;

        ExpressionExecutionData(String error, PsiExpressionCodeFragment fragment, Module module) {
            this.error = error;
            this.fragment = fragment;
            this.module = module;
        }
    }

    private void evaluateAndDisplay(PsiExpressionCodeFragment fragment, Module module) throws Exception {
        String expressionText = fragment.getText().trim();

        // Get package name from context
        String packageName = "";
        PsiElement context = fragment.getContext();
        if (context != null) {
            PsiJavaFile javaFile = (PsiJavaFile) context.getContainingFile();
            if (javaFile != null) {
                packageName = javaFile.getPackageName();
            }
        }

        // Generate wrapper class
        String simpleClassName = "ExpressionWrapper_" + System.currentTimeMillis();
        String wrapperCode = generateWrapperClass(simpleClassName, expressionText, fragment);
        String wrapperClassName = packageName.isEmpty() ? simpleClassName : packageName + "." + simpleClassName;

        // Get classpath and compile
        String classpath = buildClasspath(module);
        Class<?> wrapperClass = compileAndLoadClass(wrapperClassName, wrapperCode, classpath, module);

        // Execute the eval() method
        Method evalMethod = wrapperClass.getDeclaredMethod("eval");
        evalMethod.setAccessible(true);
        Object result = evalMethod.invoke(null);

        if (result == null) {
            SwingUtilities.invokeLater(() -> publishError("Expression returned null"));
            return;
        }

        // Render the j2html object
        String html = renderJ2HtmlObject(result);

        // Display on EDT
        SwingUtilities.invokeLater(() -> {
            publishHtmlChanged(html);
            publishSuccess("Successfully rendered HTML");
        });
    }

    private String generateWrapperClass(String className, String expression, PsiExpressionCodeFragment fragment) {
        StringBuilder code = new StringBuilder();

        // Get the package from the context
        PsiElement context = fragment.getContext();
        PsiJavaFile javaFile = null;
        if (context != null) {
            javaFile = (PsiJavaFile) context.getContainingFile();
        }

        // Add package declaration
        if (javaFile != null && !javaFile.getPackageName().isEmpty()) {
            code.append("package ").append(javaFile.getPackageName()).append(";\n");
            code.append("\n");
        }

        // Add imports from the original file
        if (javaFile != null) {
            PsiImportList importList = javaFile.getImportList();
            if (importList != null) {
                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                    code.append(importStatement.getText()).append("\n");
                }
                for (PsiImportStaticStatement importStatic : importList.getImportStaticStatements()) {
                    code.append(importStatic.getText()).append("\n");
                }
            }
            code.append("\n");
        }

        // Generate the wrapper class
        code.append("public class ").append(className).append(" {\n");
        code.append("    public static Object eval() {\n");
        code.append("        return ").append(expression).append(";\n");
        code.append("    }\n");
        code.append("}\n");

        return code.toString();
    }

    private String buildClasspath(Module module) {
        Set<String> classpathEntries = new LinkedHashSet<>();

        VirtualFile[] roots = OrderEnumerator.orderEntries(module)
            .recursively()
            .classes()
            .getRoots();

        for (VirtualFile root : roots) {
            String path = root.getPath();

            if (path.startsWith("jar://")) {
                path = path.substring(6);
            }

            int exclamation = path.indexOf("!");
            if (exclamation != -1) {
                path = path.substring(0, exclamation);
            }

            File file = new File(path);
            String normalizedPath = file.getAbsolutePath();
            classpathEntries.add(normalizedPath);
        }

        return String.join(File.pathSeparator, classpathEntries);
    }

    private JavaCompiler getProjectJavaCompiler() throws Exception {
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk == null) {
            throw new Exception("No JDK configured for this project. Please configure a JDK in File → Project Structure.");
        }

        String jdkHomePath = projectSdk.getHomePath();
        if (jdkHomePath == null) {
            throw new Exception("JDK home path not found for configured SDK: " + projectSdk.getName());
        }

        File jdkHome = new File(jdkHomePath);
        File toolsJar = new File(jdkHome, "lib/tools.jar");

        if (!toolsJar.exists()) {
            // Java 9+ - try system compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            return compiler; // May be null, which triggers process-based compilation
        }

        // Java 8 - load tools.jar
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{toolsJar.toURI().toURL()},
                ClassLoader.getSystemClassLoader()
            );

            Class<?> toolProviderClass = loader.loadClass("javax.tools.ToolProvider");
            Method getCompilerMethod = toolProviderClass.getMethod("getSystemJavaCompiler");
            JavaCompiler compiler = (JavaCompiler) getCompilerMethod.invoke(null);

            if (compiler == null) {
                throw new Exception("Failed to obtain JavaCompiler from tools.jar");
            }

            return compiler;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException | MalformedURLException e) {
            throw new Exception("Failed to load Java compiler from project JDK: " + e.getMessage(), e);
        }
    }

    private String getProjectJdkHome() {
        try {
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk != null) {
                return projectSdk.getHomePath();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get project JDK home", e);
        }
        return null;
    }

    private void compileViaProcess(Path sourceFile, String classpath, Path outputDir) throws Exception {
        String jdkHome = getProjectJdkHome();
        if (jdkHome == null) {
            throw new Exception("Cannot compile via process: JDK home path not available");
        }

        File javacFile = new File(jdkHome, "bin/javac");
        if (!javacFile.exists()) {
            javacFile = new File(jdkHome, "bin/javac.exe");
        }

        if (!javacFile.exists()) {
            throw new Exception("javac not found at: " + javacFile.getAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(javacFile.getAbsolutePath());
        command.add("-source");
        command.add("17");
        command.add("-target");
        command.add("17");
        command.add("-classpath");
        command.add(classpath);
        command.add("-d");
        command.add(outputDir.toString());
        command.add(sourceFile.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (java.io.IOException e) {
                throw new Exception("Failed to read javac output: " + e.getMessage(), e);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new Exception("Compilation failed:\n" + output.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Compilation interrupted", e);
        } catch (java.io.IOException e) {
            throw new Exception("Failed to start javac process: " + e.getMessage(), e);
        }
    }

    private Class<?> compileAndLoadClass(String className, String sourceCode, String classpath, Module module)
            throws Exception {

        JavaCompiler compiler = getProjectJavaCompiler();

        Path tempDir = Files.createTempDirectory("j2html_expr_");
        tempDir.toFile().deleteOnExit();

        // Determine package path
        String packagePath = "";
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packagePath = className.substring(0, lastDot).replace('.', '/');
            className = className.substring(lastDot + 1);
        }

        // Create source file
        Path sourceDir = tempDir;
        if (!packagePath.isEmpty()) {
            sourceDir = tempDir.resolve(packagePath);
            Files.createDirectories(sourceDir);
        }

        Path sourceFile = sourceDir.resolve(className + ".java");
        Files.writeString(sourceFile, sourceCode);

        if (compiler != null) {
            // Use JavaCompiler API
            List<String> options = new ArrayList<>();
            options.add("-source");
            options.add("17");
            options.add("-target");
            options.add("17");
            options.add("-classpath");
            options.add(classpath);
            options.add("-d");
            options.add(tempDir.toString());

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile.toFile()));

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                StringBuilder errors = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errors.append(String.format("Line %d: %s\n",
                        diagnostic.getLineNumber(),
                        diagnostic.getMessage(null)));
                }
                throw new Exception(errors.toString());
            }
        } else {
            // Fall back to process-based compilation
            LOG.info("Using process-based javac compilation");
            compileViaProcess(sourceFile, classpath, tempDir);
        }

        // Load the compiled class
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{tempDir.toUri().toURL()},
            getModuleClassLoader(module)
        );

        String fullClassName = packagePath.isEmpty() ? className : packagePath.replace('/', '.') + '.' + className;
        return classLoader.loadClass(fullClassName);
    }

    @Override
    public void dispose() {
        psiChangeAlarm.cancelAllRequests();
    }
}
