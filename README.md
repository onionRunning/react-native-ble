# react-native-ble

ble

## Installation

```sh
npm install react-native-ble
```

```sh
yarn add react-native-ble
```

## Usage

```js
import {bleModuleApi} from 'react-native-ble'

// ...

const getPermissions = async () => {
  const res = await bleModuleApi.judgePermissionOk()
  // {data: '', code: 0/1/200}
  // code === 0, 当前设备不支持蓝牙权限
  // code === 1, 当前设备没开启蓝牙权限
  // code === 2, 设备没开启蓝牙扫描权限
  // code === 200 , 当前设备权限开启
}

const requestPermission = async () => {
  const res = await bleModuleApi.requestPermission()

  // res === true 请求权限成功
  // res === false 请求权限失败
}

// 开始连接蓝牙
const startScan = () => {
  // 连接的蓝牙名
  bleModuleApi.startConnectBleFn('70129G01716NY', data => {
    if (data.code === 200) {
      setConnectResult('连接成功!')
      return
    }
    setConnectResult('连接失败!')
  })
}

// 开始断开蓝牙
const disconnect = () => {
  bleModuleApi.disConnectBle('70129G01716NY')
}

// 发送指令到蓝牙设备
// 注意这里的协议需要你与硬件端进行沟通，通信协议
const sendTextToBle = async () => {
  // 发送通知给蓝牙
  const res = await bleModuleApi.sendCommandToBle([
    'cc',
    'cc',
    '00',
    '02',
    '02',
    '01',
    '00',
    '00',
  ])
  console.info(res, 'get info')
}

...

```

- **android需要进行集成**

> android/app/src/main/AndroidManifest.xml 添加相关权限

```AndroidManifest.xml
...
    <uses-feature android:name="android.hardware.bluetooth_le" android:required= "true" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <!--Android 12需要的权限 -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"  android:usesPermissionFlags="neverForLocation"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

> 引用插件 android/app/build.gradle

```build.gradle

    implementation project(':react-native-ble-5e')

```

> MainApplication.kt

```MainApplication.kt

...
import com.ble.BleManager
import com.ble.EventEmitter



...
override fun onCreate() {
  ...
  EventEmitter.install(reactNativeHost)
  BleManager.install(this)
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

## 开发

```md

npx create-react-native-library react-native-ble
```
