import * as React from 'react'
import {StyleSheet, View, Text, TouchableOpacity} from 'react-native'
import {bleModuleApi} from 'react-native-ble'

export default function App() {
  const [result, setResult] = React.useState<string>('')

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
