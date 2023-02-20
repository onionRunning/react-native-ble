import * as React from 'react'
import {StyleSheet, View, Text, TouchableOpacity, TextInput} from 'react-native'
import {bleModuleApi} from '../../src/index'

export default function App() {
  const [result, setResult] = React.useState<string>('')
  const [connectResult, setConnectResult] = React.useState('')
  const value = React.useRef('')
  const [bleRes, setBleRes] = React.useState('')

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
    setConnectResult('断开连接!')
  }

  const sendTextToBle = async () => {
    // 发送通知给蓝牙
    console.info(value.current, '===2===3===4===')
    const res = await bleModuleApi.sendCommandToBle([
      'cc',
      'cc',
      '00',
      '02',
      '02',
      '02',
      '00',
      '00',
    ])
    console.info(res, '-')
    console.info(res?.data, 'get info')
    setBleRes(res?.data?.reason || res?.data)
  }

  const changeText = (e: string) => {
    // console.info(e, '--------2--------2-------')
    value.current = e
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
          <Text>开始扫码且连接</Text>
        </TouchableOpacity>
      ) : (
        <View />
      )}

      <TouchableOpacity onPress={disconnect} style={styles.touch}>
        <Text>断开蓝牙连接</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={sendTextToBle} style={styles.touch}>
        <Text>发送通知给蓝牙</Text>
      </TouchableOpacity>
      <View>
        <Text>蓝牙响应</Text>
        <TextInput disableFullscreenUI style={styles.input} value={bleRes} />
      </View>
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
  input: {
    width: '80%',
    height: 48,
    borderColor: '#fa0',
    borderWidth: 1,

    margin: 20,
    padding: 10,
  },
})
