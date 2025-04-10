package dev.idachev.recipeservice.infrastructure;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility to access Spring beans from non-Spring contexts or circular dependency situations.
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
    
    /**
     * Get a bean by type
     * @param <T> The bean type
     * @param beanClass The class of the required bean
     * @return The bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        return context.getBean(beanClass);
    }
    
    /**
     * Get a bean by name and type
     * @param <T> The bean type
     * @param name The bean name
     * @param beanClass The class of the required bean
     * @return The bean instance
     */
    public static <T> T getBean(String name, Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        return context.getBean(name, beanClass);
    }
} 