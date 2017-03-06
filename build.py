#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import sys
import time
from subprocess import Popen, PIPE
import zipfile
import platform
import getpass

GRADLE_CMD = 'gradle'
ACTION = 'build'
BUILD_MODULES = ['latte-class-recorder', 'latte-compiler', 'latte-gradle-plugin', 'latte-library', 'latte-build']
DEPLOY_USER = ''
DEPLOY_PASS = ''

OS = ''

JAVA_MIN_VERSION = 1.6
GRADLE_MIN_VERSION = 2.14
PACK_ZIP_DIR = 'latte-build/build/distributions/'
VERSION_FILE = 'latte-build/src/main/resources/version'

def log(s):
    print s

def javaVersion():
    try:
        ps = Popen('java -version'.split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return 0
    lines = ps.stderr.readlines()
    if len(lines) == 0:
        return 0
    ver = lines[0].strip()
    starter = 'java version \"'
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):-1]
        while '_' in ver:
            ver = ver[0:ver.find('_')]
        if '.' in ver and ver.find('.', 2) != -1:
            ver = ver[0:ver.find('.', 2)]
        return float(ver)
    else:
        return 0

def gradleVersion():
    try:
        ps = Popen((GRADLE_CMD + ' --version').split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return 0
    lines = ps.stdout.readlines()
    if len(lines) < 3:
        return 0
    ver = lines[2].strip()
    starter = 'Gradle '
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):-1]
        if '.' in ver and ver.find('.', 2) != -1:
            ver = ver[0:ver.find('.', 2)]
        return float(ver)
    else:
        return 0

def check():
    # check java version
    jVer = javaVersion()
    if jVer == 0:
        log('Java not found')
        return False
    if jVer < JAVA_MIN_VERSION:
        log('Java version is %s, but %s or higher required' % (str(jVer), str(JAVA_MIN_VERSION)))
        return False

    # check gradle version
    gVer = gradleVersion()
    if gVer == 0:
        log('Gradle not found [' + GRADLE_CMD + ']')
        return False
    if gVer < GRADLE_MIN_VERSION:
        log('Gradle version is %s, but %s or higher required' % (str(gVer), str(GRADLE_MIN_VERSION)))
        return False

    # get version
    versionFile = open(VERSION_FILE, 'r')
    version = versionFile.read().strip()
    versionFile.close()
    log('--- current version is [%s] ---' % (version))

    # os
    system = platform.system()
    log('--- current os is [%s] ---' % (system))
    global OS
    OS = system

    return True

def execute(cmd):
    exportStr = 'export'
    if OS == 'windows':
        exportStr = 'set'
    env = {}
    env['BUILD_ACTION'] = ACTION
    if DEPLOY_USER and DEPLOY_PASS:
        env['DEPLOY_USER'] = DEPLOY_USER
        env['DEPLOY_PASS'] = DEPLOY_PASS
    s = ''
    for k in env:
        s += (exportStr + ' ' + k + '=' + env[k] + '\n')
    return 0 == os.system(s + cmd)

def buildModule(module):
    log('--- Start to build module [%s] ---' % (module))
    return execute('cd %s\n%s clean latteBuild' % (module, GRADLE_CMD))

def build():
    for m in BUILD_MODULES:
        if not buildModule(m):
            return False
    return True

def winLink(script):
    # TODO windows link
    return True

def unixLink(script):
    # some file system do not support `ln`
    # so we create a new shell script
    # which redirect all input arguments to dest script
    f = open('latte', 'w')
    f.write('basepath=$(cd `dirname $0`; pwd)\n')
    f.write('$basepath/' + script + ' $*\n')
    f.close()
    # and chmox +x to both the scripts
    return \
    (0 == os.system('chmod +x latte')) and \
    (0 == os.system('chmod +x ' + script))

def scripts():
    # unzip the latte.jar
    zipF = None
    for f in os.listdir(PACK_ZIP_DIR):
        if f[-4:] == '.zip':
            zipF = f
            break
    if not zipF:
        log('distribution zip file not found')
        return False
    theFile = zipfile.ZipFile(PACK_ZIP_DIR + zipF, 'r')
    for filename in theFile.namelist():
        theFile.extract(filename, "./build/")

    # link the scripts
    script = 'build/' + zipF[0:-4] + '/bin/latte'
    system = platform.system()

    linkMethod = None
    if system == 'windows':
        linkMethod = winLink
    else:
        linkMethod = unixLink

    if linkMethod(script):
        return True
    else:
        log('Create shortcut script failed')
        return False

def buildStart():
    log('===================================')
    log('            Build Start            ')
    log('===================================')
    startTime = time.time()
    res = check() and build() and scripts()
    endTime = time.time()
    if res:
        log('===================================')
        log('          Build Successful         ')
        log('===================================')
        log('Total time: %.3f secs' % (endTime - startTime))
    else:
        log('===================================')
        log('           Build Failed            ')
        log('===================================')

def testModule(module):
    log('--- Start to test module [%s] ---' % (module))
    return execute('cd %s\n%s clean latteTest' % (module, GRADLE_CMD))

def test():
    for m in BUILD_MODULES:
        if not testModule(m):
            return False
    return True

def testStart():
    log('===================================')
    log('            Test Start             ')
    log('===================================')
    startTime = time.time()
    res = check() and test()
    endTime = time.time()
    if res:
        log('===================================')
        log('          Test Successful          ')
        log('===================================')
        log('Total time: %.3f secs' % (endTime - startTime))
    else:
        log('===================================')
        log('            Test Failed            ')
        log('===================================')

def deployModule(module):
    log('--- Start to build module [%s] ---' % (module))
    return execute('cd %s\n%s clean latteDeploy' % (module, GRADLE_CMD))

def deploy():
    for m in BUILD_MODULES:
        if not deployModule(m):
            return False
    return True

def deployStart():
    log('===================================')
    log('           Deploy Start            ')
    log('===================================')
    startTime = time.time()
    res = check() and deploy()
    endTime = time.time()
    if res:
        log('===================================')
        log('         Deploy Successful         ')
        log('===================================')
        log('Total time: %.3f secs' % (endTime - startTime))
    else:
        log('===================================')
        log('           Deploy Failed           ')
        log('===================================')

def assertNotLast(argv, i):
    if len(argv) > i + 1:
        return
    raise Exception('command not complete')

def extractArgs():
    if len(sys.argv) == 1:
        return {}

    global GRADLE_CMD
    global ACTION
    global BUILD_MODULES

    index = 1
    while index < len(sys.argv):
        cmd = sys.argv[index]
        if cmd == '--gradle' or cmd == '-g':
            assertNotLast(sys.argv, index)
            index = index + 1
            GRADLE_CMD = sys.argv[index]

        elif cmd == '--action' or cmd == '-a':
            assertNotLast(sys.argv, index)
            index = index + 1
            ACTION = sys.argv[index]

        elif cmd == '--modules' or cmd == '-m':
            assertNotLast(sys.argv, index)
            index = index + 1
            BUILD_MODULES = sys.argv[index].split(',')

        else:
            raise Exception('unknown arg [' + cmd + ']')
        index = index + 1

if __name__ == "__main__":
    if len(sys.argv) == 2 and ((sys.argv[1] == '-h') or (sys.argv[1] == '--help')):
        log('''./build.py [-g|--gradle gradle-command-name]
           [-a|--action build|deploy|test]
           [-m|--modules module-name1,module-name2,...]''')
        exit()
    try:
        extractArgs()
        if ACTION == 'build':
            buildStart()
        elif ACTION == 'deploy':
            DEPLOY_USER = raw_input('user: ')
            DEPLOY_PASS = getpass.getpass('pass: ')
            deployStart()
        elif ACTION == 'test':
            testStart()
        else:
            log('Unknown action [' + ACTION + ']')
    except Exception, e:
        log(str(e))
