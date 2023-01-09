import * as React from 'react'
import {StyleSheet, View, Text, TouchableOpacity} from 'react-native'
import {bleModuleApi} from '../../src/index'

export default function App() {
  const [result, setResult] = React.useState<string>('')
  const [connectResult, setConnectResult] = React.useState('')

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
    // bleModuleApi.startScanBle()
    // setStart(true)
    bleModuleApi.startConnectBleFn('70129G01716NY', data => {
      console.info(data, '-------')
      if (data.code === 200) {
        setConnectResult('连接成功!')
        return
      }
      setConnectResult('连接失败!')
    })
  }

  const disconnect = () => {
    bleModuleApi.disConnectBle('70129G01716NY')
  }

  const isNeedRequest = result === '暂未申请权限!'
  return (
    <View style={styles.container}>
      <Text>设备蓝牙扫描状态: {result}</Text>
      <Text>当前蓝牙连接状态: {connectResult}</Text>
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

      <TouchableOpacity onPress={disconnect} style={styles.touch}>
        <Text>断开蓝牙连接</Text>
      </TouchableOpacity>
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
