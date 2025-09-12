package fansirsqi.xposed.sesame.task;

import android.os.Build;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelType;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

public abstract class ModelTask extends Model {
    private static final Map<ModelTask, Thread> MAIN_TASK_MAP = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor MAIN_THREAD_POOL =
            new ThreadPoolExecutor(getModelArray().length, Integer.MAX_VALUE,
                    30L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadPoolExecutor.CallerRunsPolicy());

    private final Map<String, ChildModelTask> childTaskMap = new ConcurrentHashMap<>();
    private ChildTaskExecutor childTaskExecutor;
    private int run_cnts = 0;

    @Getter
    private final Runnable mainRunnable = new Runnable() {
        private final ModelTask task = ModelTask.this;
        @Override
        public void run() {
            if (MAIN_TASK_MAP.get(task) != null) {
                return;
            }
            MAIN_TASK_MAP.put(task, Thread.currentThread());
            try {
                Notify.setStatusTextExec(task.getName());
                task.run();
            } catch (Exception e) {
                Log.printStackTrace(e);
            } finally {
                MAIN_TASK_MAP.remove(task);
                Notify.updateNextExecText(-1);
            }
        }
    };

    public ModelTask() {}

    public void addRunCnts() {
        run_cnts += 1;
    }

    public int getRunCnts() {
        return run_cnts;
    }

    /**
     * 准备任务执行环境
     */
    @Override
    public final void prepare() {
        childTaskExecutor = newTimedTaskExecutor();
    }

    /**
     * 确保 childTaskExecutor 初始化
     */
    private void ensureChildTaskExecutor() {
        if (childTaskExecutor == null) {
            childTaskExecutor = newTimedTaskExecutor();
        }
    }

    public String getId() {
        return toString();
    }

    public ModelType getType() {
        return ModelType.TASK;
    }

    public abstract String getName();

    public abstract ModelFields getFields();

    public abstract Boolean check();

    public Boolean isSync() {
        return true;
    }

    public abstract void run();

    public Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }

    public ChildModelTask getChildTask(String childId) {
        return childTaskMap.get(childId);
    }

    /**
     * 添加子任务
     */
    public void addChildTask(ChildModelTask childTask) {
        ensureChildTaskExecutor();
        String childId = childTask.getId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    value.cancel();
                }
                childTask.setModelTask(this);
                if (childTaskExecutor.addChildTask(childTask)) {
                    return childTask;
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask oldTask = childTaskMap.get(childId);
                if (oldTask != null) {
                    oldTask.cancel();
                }
                childTask.setModelTask(this);
                if (childTaskExecutor.addChildTask(childTask)) {
                    childTaskMap.put(childId, childTask);
                }
            }
        }
    }

    /**
     * 移除子任务
     */
    public void removeChildTask(String childId) {
        ensureChildTaskExecutor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    childTaskExecutor.removeChildTask(value);
                }
                return null;
            });
        } else {
            synchronized (childTaskMap) {
                ChildModelTask childTask = childTaskMap.get(childId);
                if (childTask != null) {
                    childTaskExecutor.removeChildTask(childTask);
                }
                childTaskMap.remove(childId);
            }
        }
    }

    public Integer countChildTask() {
        return childTaskMap.size();
    }

    public Boolean startTask() {
        return startTask(false);
    }

    public synchronized Boolean startTask(Boolean force) {
        if (MAIN_TASK_MAP.containsKey(this)) {
            if (!force) {
                return false;
            }
            stopTask();
        }
        try {
            if (isEnable() && check()) {
                prepare(); // 启动前准备执行器
                if (isSync()) {
                    mainRunnable.run();
                } else {
                    MAIN_THREAD_POOL.execute(mainRunnable);
                }
                return true;
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        return false;
    }

    public synchronized void stopTask() {
        for (ChildModelTask childModelTask : childTaskMap.values()) {
            try {
                childModelTask.cancel();
            } catch (Exception e) {
                Log.printStackTrace(e);
            }
        }
        if (childTaskExecutor != null) {
            childTaskExecutor.clearAllChildTask();
            childTaskExecutor = null; // 清理引用，避免旧实例残留
        }
        childTaskMap.clear();
        MAIN_THREAD_POOL.remove(mainRunnable);
        MAIN_TASK_MAP.remove(this);
    }

    public static void startAllTask() {
        startAllTask(false);
    }

    public static void startAllTask(Boolean force) {
        Notify.setStatusTextExec();
        for (int run_cnt = 1; run_cnt <= 2; run_cnt++) {
            Log.record("第" + run_cnt + "轮开始");
            for (Model model : getModelArray()) {
                if (model != null) {
                    if (ModelType.TASK == model.getType()) {
                        ((ModelTask) model).addRunCnts();
                        int model_priority = ((ModelTask) model).getPriority();
                        if (run_cnt < model_priority) {
                            Log.record("模块[" + ((ModelTask) model).getName() + "]优先级:" + model_priority + " 第" + run_cnt + "轮跳过");
                            continue;
                        }
                        if (((ModelTask) model).startTask(force)) {
                            GlobalThreadPools.sleep(10);
                        }
                    }
                }
            }
            Log.record("第" + run_cnt + "轮结束");
        }
    }

    public static void stopAllTask() {
        for (Model model : getModelArray()) {
            if (model != null) {
                try {
                    if (ModelType.TASK == model.getType()) {
                        ((ModelTask) model).stopTask();
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }
    }

    private ChildTaskExecutor newTimedTaskExecutor() {
        ChildTaskExecutor childTaskExecutor;
        Integer timedTaskModel = BaseModel.getTimedTaskModel().getValue();
        if (timedTaskModel == BaseModel.TimedTaskModel.SYSTEM) {
            childTaskExecutor = new SystemChildTaskExecutor();
        } else if (timedTaskModel == BaseModel.TimedTaskModel.PROGRAM) {
            childTaskExecutor = new ProgramChildTaskExecutor();
        } else {
            throw new RuntimeException("not found childTaskExecutor");
        }
        return childTaskExecutor;
    }

    @Getter
    public static class ChildModelTask implements Runnable {
        @Setter
        private ModelTask modelTask;
        private final String id;
        private final String group;
        private final Runnable runnable;
        private final Long execTime;
        private CancelTask cancelTask;
        private Boolean isCancel = false;

        protected ChildModelTask(String id, long execTime) {
            this(id, null, null, execTime);
        }

        public ChildModelTask(String id, Runnable runnable) {
            this(id, null, runnable, 0L);
        }

        public ChildModelTask(String id, String group, Runnable runnable) {
            this(id, group, runnable, 0L);
        }

        public ChildModelTask(String id, String group, Runnable runnable, Long execTime) {
            if (StringUtil.isEmpty(id)) {
                id = toString();
            }
            if (StringUtil.isEmpty(group)) {
                group = "DEFAULT";
            }
            if (runnable == null) {
                runnable = setRunnable();
            }
            this.id = id;
            this.group = group;
            this.runnable = runnable;
            this.execTime = execTime;
        }

        public Runnable setRunnable() {
            return null;
        }

        public final void run() {
            getRunnable().run();
        }

        protected void setCancelTask(CancelTask cancelTask) {
            this.cancelTask = cancelTask;
        }

        public final void cancel() {
            if (getCancelTask() != null) {
                try {
                    getCancelTask().cancel();
                    setCancel(true);
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
        }

        public Boolean getCancel() {
            return isCancel;
        }

        public void setCancel(Boolean cancel) {
            isCancel = cancel;
        }
    }

    public interface CancelTask {
        void cancel();
    }
}