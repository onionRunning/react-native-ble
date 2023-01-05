import * as React from 'react'

import {StyleSheet, View, Text} from 'react-native'
import {bleModuleApi} from 'react-native-ble'

export default function App() {
  const [result, setResult] = React.useState<number | undefined>()

  React.useEffect(() => {
    getPermission()
  }, [])

  const getPermission = async () => {
    const res = await bleModuleApi.isPermissionOk()
    console.info(res, '======')
  }

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
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
})
