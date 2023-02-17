//
//  Util.m
//  Ble
//
//  Created by 王聪 on 2023/2/17.
//  Copyright © 2023 Facebook. All rights reserved.
//

//#import <Foundation/Foundation.h>
#import "Util.h"
// #import "Config.h"

@implementation Util

// 获取响应数据的长度
+ (NSInteger)fetchLengthFromResponse:(NSData *)data {
    NSData *lengthData = [data subdataWithRange:NSMakeRange(2, 2)]; // 0x cc cc (00 03) 86 00 00

    unsigned length = 0;
    NSScanner *scanner = [NSScanner scannerWithString:[self hexString:lengthData]];
    [scanner scanHexInt:&length];
    return length;
}

+ (NSInteger)fetchOrderTypeFromResponse:(NSData *)data {
    NSData *typeData = [data subdataWithRange:NSMakeRange(4, 1)]; // 0x cc cc 00 03 (86) 00 00

    unsigned type = 0;
    NSScanner *scanner = [NSScanner scannerWithString:[self hexString:typeData]];
    [scanner scanHexInt:&type];
    return type;
}

// data 转 字符串
+ (NSString *)hexString:(NSData *)data {
     NSMutableData *result = [NSMutableData dataWithLength:2 * data.length];
     unsigned const char* src = data.bytes;
     unsigned char* dst = result.mutableBytes;
     unsigned char t0, t1;

     for (int i = 0; i < data.length; i ++ ) {
          t0 = src[i] >> 4;
          t1 = src[i] & 0x0F;

          dst[i*2] = 48 + t0 + (t0 / 10) * 39;
          dst[i*2+1] = 48 + t1 + (t1 / 10) * 39;
     }

     return [[NSString alloc] initWithData:result encoding:NSASCIIStringEncoding];
}

+ (NSString *)textFrom:(NSString *)hexString {
    NSMutableString *newString = [[NSMutableString alloc] init];
    int i = 0;
    while (i < [hexString length]){
        NSString * hexChar = [hexString substringWithRange: NSMakeRange(i, 2)];
        int value = 0;

        sscanf([hexChar cStringUsingEncoding:NSASCIIStringEncoding], "%x", &value);
        [newString appendFormat:@"%c", (char)value];
        i+=2;
    }
    return newString;
}

+ (NSData *)convertHexToData:(NSArray<NSString *> *)command {
    NSMutableString *temp = [NSMutableString string];
    for (NSString *c in command) {
        [temp appendString:c];
    }
    NSMutableData *commandToSend = [[NSMutableData alloc] init];
    unsigned char whole_byte;
    char byte_chars[3] = {'\0','\0','\0'};
    for (int i = 0; i < ([temp length] / 2); i++) {
        byte_chars[0] = [temp characterAtIndex:i*2];
        byte_chars[1] = [temp characterAtIndex:i*2+1];
        whole_byte = strtol(byte_chars, NULL, 16);
        [commandToSend appendBytes:&whole_byte length:1];
    }
    return [commandToSend copy];
}

+ (NSNumber *)seperatePage:(NSArray<NSString *> *)command {
      if ([command count] >= 8) {
          if ([command[4] isEqualToString:@"0a"] || [command[4] isEqualToString:@"07"]) {
              NSString *page = [NSString stringWithFormat:@"%@%@", command[5], command[6]];
              unsigned result = 0;
              NSScanner *scanner = [NSScanner scannerWithString:page];
              [scanner scanHexInt:&result];
              return @(result);
          }
      }
    return @0;
}

+ (NSNumber *)seperateIndex:(NSArray<NSString *> *)command {
      if ([command count] >= 8) {
          if ([command[4] isEqualToString:@"0a"] || [command[4] isEqualToString:@"07"]) {
              NSString *index = command[7];
              unsigned result = 0;
              NSScanner *scanner = [NSScanner scannerWithString:index];
              [scanner scanHexInt:&result];
              return @(result);
          }
      }
    return @0;
}

// + (void)getStoreVersionAndLoaclVersion:(Result)result {
//     NSString* urlStr = [NSString stringWithFormat:@"http://itunes.apple.com/lookup?id=%@", APPSTORE_ID];
//     NSURL *url = [NSURL URLWithString:urlStr];
//     NSURLRequest *request = [NSURLRequest requestWithURL:url];
//     NSURLSession *session = [NSURLSession sharedSession];
//     NSURLSessionDataTask *dataTask = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
//         if(!error) {
//             NSDictionary *dict = [Util jsonData2Dictionary:data];
//             if ([dict objectForKey:@"resultCount"] != [NSNull null]) {
//                 NSNumber *resultCount = dict[@"resultCount"];
//                 if ([resultCount isEqualToNumber:@0]) {
//                     result(@[]);
//                     return;
//                 }
//                 NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
//                 NSString *appVersion = [infoDictionary objectForKey:@"CFBundleShortVersionString"];
//                 NSString *appStoreVersion = dict[@"results"][0][@"version"];
//                 NSArray *res = @[appStoreVersion, appVersion];
//                 result(res);
//                 return;
//             }
//         }
//     }];
//     [dataTask resume];
// }

+ (VersionCompare)compareStoreVersion:(NSString *)versionAppStore localVersion:(NSString *)versionLocal {
    NSArray *arrayAppStore = [versionAppStore componentsSeparatedByString:@"."];
    NSArray *arrayLocal = [versionLocal componentsSeparatedByString:@"."];
    NSInteger shortCount = arrayAppStore.count<arrayLocal.count?arrayAppStore.count:arrayLocal.count;

    for (NSInteger i = 0; i < shortCount; i++) {
        if ([arrayAppStore[i] integerValue] > [arrayLocal[i] integerValue]) {
            // App Store版本高，需要升级
            return VersionCompareNeedUpdate;
        } else if ([arrayAppStore[i] integerValue] == [arrayLocal[i] integerValue]) {
            continue;
        } else {
            // App Store版本低，需要显示邮箱登录
            return VersionCompareShowMailLogin;
        }
    }
    // 在相同位数下没有得到结果，那么位数多的版本高
    if (arrayAppStore.count > arrayLocal.count) {
        return VersionCompareNeedUpdate;
    } else {
        return VersionCompareUnNeedUpdate;
    }
}

+ (NSDictionary *)jsonData2Dictionary:(NSData *)jsonData {
    if (jsonData == nil) {
        return nil;
    }
    NSError *err = nil;
    NSDictionary *dic = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:&err];
    if (err || ![dic isKindOfClass:[NSDictionary class]]) {
        NSLog(@"Json parse failed");
        return nil;
    }
    return dic;
}

+ (NSString *)addSpacing:(NSString *)input {
    NSMutableString *copied = [NSMutableString string];
    for (int i = 0; i < [input length]; i = i + 2) {
        [copied appendString:[input substringWithRange:NSMakeRange(i, 2)]];
        if (i != [input length] - 2) {
            [copied appendString:@" "];
        }
    }
    return [copied copy];
}

@end
