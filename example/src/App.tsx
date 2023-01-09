import * as React from 'react'
import {StyleSheet, View, Text, TouchableOpacity} from 'react-native'
import {bleModuleApi} from '../../src/index'

export default function App() {
  const [result, setResult] = React.useState<string>('')
  const [isStart, setStart] = React.useState(false)

  console.info(bleModuleApi, 'hello')
  React.useEffect(() => {
    getPermission()
  }, [])

  const getPermission = async () => {
    const res = await bleModuleApi.judgePermissionOk()
    console.info(res, '======')
    if (res.code !== 200) {
      setResult('暂未申请权限!')
    }
    if (res.code === 200) {
      setResult('当前具有蓝牙权限!')
    }
  }

  // 请求权限
  const requestPermission = async () => {
    console.info('request permission!')
    const res = await bleModuleApi.requestPermission()
    if (res) {
      setResult('当前具有蓝牙权限!')
    }
    console.info(res, '--------')
  }

  const startScan = () => {
    bleModuleApi.startScanBle()
    setStart(true)
  }

  const getResult = (res: any) => {
    console.info(res, '------------')
  }

  React.useEffect(() => {
    if (!isStart) return
    // bleModuleApi.requestScanResultListener(getResult)
    // return () => {
    //   bleModuleApi.removeScanResultListener()
    // }
  }, [isStart])

  const isNeedRequest = result === '暂未申请权限!'
  return (
    <View style={styles.container}>
      <Text>设备蓝牙扫描状态: {result}</Text>
      {isNeedRequest ? (
        <TouchableOpacity onPress={requestPermission} style={styles.touch}>
          <Text>申请Ble权限</Text>
        </TouchableOpacity>
      ) : (
        <View />
      )}

      {!isNeedRequest ? (
        <TouchableOpacity onPress={startScan} style={styles.touch}>
          <Text>开始扫码</Text>
        </TouchableOpacity>
      ) : (
        <View />
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  touch: {
    marginTop: 50,
    backgroundColor: '#fa0',
    padding: 20,
    borderRadius: 10,
  },
})
