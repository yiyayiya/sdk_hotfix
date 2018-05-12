# sdk 热更新

> 在工作中有开发sdk，但是使用 sdk 的 app 开发不在我们这边，app 的版本发布也不受我们控制，每次 sdk 的 bug 修复或者优化都需要等待 app 下一个版本的发布，因此如果 sdk 可以热更新的话，bug的修复、功能优化的验证可以更快的响应。
> 虽然sdk 热更新的实现很简单，但是知易行难，sdk 热更新这个功能一直都还没有上。这次终于把它做了。

###1. sdk 热更新的原理

原理很简单，就是利用 android 的 DexClassLoader 对 sdk 源码编译后的 jar 进行动态加载。这个 jar 可以从服务器下载最新的版本，因此可以在不发布 app 的条件下对 sdk 进行热更新以修复bug或者进行功能优化。

###2. sdk 热更新框架设计

#### 2.1 项目结构
1. sdk module ,sdk 的实现，这部分可以热修复。
2. sdk_shell, sdk 的壳，提供给app 调用的接口，这部分和app 一起编译打包不能热修复。
3. app ，app 依赖 sdk_shell module。

####2.2 sdk 接口设计

范例中 sdk 对外部提供下面接口

```java
public interface ISdkApi {
    void showToast(Context context);
}
```

sdk 接口实现如下：
```java
public class SdkImpl implements ISdkApi {
    @Override
    public void showToast(Context context) {
        Toast.makeText(context, "hello word ! 3344", Toast.LENGTH_LONG).show();
    }
}

```

####2.3 sdk_shell 实现Proxy接口，提供给app调用的接口

1. sdk_shell 有一个同样的ISdkApi，接口名、包名和 sdk 中的 ISdkApi 完全一样，目的是通过反射获取到sdk 中的 SdkImpl 实例后，方便调用 sdk 的接口。

```java
public interface ISdkApi {
    void showToast(Context context);
}
```

2. 写一个SdkApiProxy 实现 ISdkApi。

```java
public class SdkApiProxy implements ISdkApi {

    private ISdkApi mSdkApi;

    public void init(Context context, String dexPath, String implCalssName)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        File file = new File(dexPath);
        File dexOutputDir = context.getDir("dex", 0);
        DexClassLoader classLoader = new DexClassLoader(file.getAbsolutePath(),
                dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        Class sdkClass = classLoader.loadClass(implCalssName);
        mSdkApi = (ISdkApi) sdkClass.newInstance();
    }

    @Override
    public void showToast(Context context) {
        mSdkApi.showToast(context);
    }

}

```
3. app 端使用sdk

3.1 加载sdk
```java
 private void loadSdk(){
        mHotFixProxy = new SdkApiProxy();
        boolean initSdkSuccess = false;
        try {
            //加载sdk 实例
            mHotFixProxy.init(getApplicationContext(), "/sdcard/sdk_dex.jar", "com.qding.hotfix.HotFixImpl");
            initSdkSuccess = true;
        } catch (ClassNotFoundException e) {
            //类找不到异常
            e.printStackTrace();
            showException(e);
        } catch (IllegalAccessException e) {
            //创建实例权限异常,一般来说，是由于java在反射时调用了private方法所导致的
            e.printStackTrace();
            showException(e);
        } catch (InstantiationException e) {
            //创建实例异常,当试图通过newInstance()方法创建某个类的实例,而该类是一个抽象类或接口时,抛出该异常
            e.printStackTrace();
            showException(e);
        } finally {
            if (!initSdkSuccess) {
                mHotFixProxy = null;
            }
        }
    }
```

3.2 通过Proxy 调用sdk 接口

```java
findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHotFixProxy == null) {
                    showException(new RuntimeException("sdk 加载失败"));
                    return;
                }
                mHotFixProxy.showToast(getBaseContext());
            }
        });
```

###3. 热更新测试

#### 3.1 sdk 编译成 jar ,并转换为 android 可以加载的dex 格式的jar 

android studio 中 Build-》Make Module sdk 即可以编译jar，然后使用下面gradle 脚本 可以将编译的 jar 复制到指定问题并重命名:

```groovy
task deleteOldJar(type: Delete) {
    delete 'build/libs/'
}

task exportJar(type: Copy) {
    from('build/intermediates/intermediate-jars/release')
    into('build/libs/')
    include('classes.jar')
    rename('classes.jar', 'sdk.jar')
}
exportJar.dependsOn(deleteOldJar, build)
```  

其中路径 build/intermediates/intermediate-jars/release 不同版本的com.android.tools.build:gradle 可能会不一样。
##### 转换为 android 可以加载的dex 格式的jar:
转换为dex 格式的jar 需要用到 android sdk build-tools 里面的dx工具,转换命令如下：

```shell
dx --dex --output=target.jar origin.jar
```
其中target.jar 是转换后的dex 格式化jar 包，origin.jar 是需要转换的jar。

#### 3.2 更新sdk
修改 sdk SdkImpl 中showToast 的内容，编译并转换为dex 格式化的jar 后，adb push 到 /sdcard/sdk_dex.jar 进行测试，再不需要更新app，也无需重启app 的情况下，可以进行sdk 的修改更新。




