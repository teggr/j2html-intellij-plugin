package com.example.j2htmlpreview;

import com.intellij.openapi.Disposable;
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
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
                    // TODO: Implement expression execution
                    publishError("Expression execution not yet implemented in refactored version");
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
        return returnTypeName.equals("ContainerTag") ||
               returnTypeName.equals("DomContent") ||
               returnTypeName.equals("Tag") ||
               returnTypeName.equals("UnescapedText") ||
               returnTypeName.equals("Text");
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
    
    @Override
    public void dispose() {
        psiChangeAlarm.cancelAllRequests();
    }
}
