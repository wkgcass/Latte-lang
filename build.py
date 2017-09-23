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
ALLOWED_ACTIONS = ['build', 'test', 'deploy']
BUILD_MODULES = ['latte-class-recorder', 'latte-compiler', 'latte-gradle-plugin', 'latte-library', 'latte-build']
DEPLOY_USER = ''
DEPLOY_PASS = ''

JAVA_MIN_VERSION = '1.6'
GRADLE_MIN_VERSION = '2.12'
PACK_ZIP_DIR = 'latte-build/build/distributions/'
VERSION_FILE = 'latte-build/src/main/resources/version'

def log(s):
    print s

def javaVersion():
    try:
        ps = Popen('java -version'.split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return ''
    lines = ps.stderr.readlines()
    if len(lines) == 0:
        return ''
    ver = lines[0].strip()
    starter = 'java version \"'
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):-1]
        while '_' in ver:
            ver = ver[0:ver.find('_')]
        return ver
    else:
        return ''

def gradleVersion():
    try:
        ps = Popen((GRADLE_CMD + ' --version').split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return ''
    lines = ps.stdout.readlines()
    if len(lines) < 3:
        return ''
    ver = lines[2].strip()
    starter = 'Gradle '
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):]
        return ver
    else:
        return ''

def compareVersionLt(a, b):
    aa = a.split('.')
    bb = b.split('.')
    i = 0
    while i < len(aa) and i < len(bb):
        ia = int(aa[i])
        ib = int(bb[i])
        i += 1
        if ia > ib:
            return False
        elif ia == ib:
            continue
        else:
            return True
    return False

def check():
    # check java version
    jVer = javaVersion()
    if jVer == '':
        log('Java not found')
        return False
    if compareVersionLt(jVer, JAVA_MIN_VERSION):
        log('Java version is %s, but %s or higher required' % (str(jVer), str(JAVA_MIN_VERSION)))
        return False

    # check gradle version
    gVer = gradleVersion()
    if gVer == '':
        log('Gradle not found [' + GRADLE_CMD + ']')
        return False
    if compareVersionLt(gVer, GRADLE_MIN_VERSION):
        log('Gradle version is %s, but %s or higher required' % (str(gVer), str(GRADLE_MIN_VERSION)))
        return False

    # get version
    versionFile = open(VERSION_FILE, 'r')
    version = versionFile.readline().strip()
    versionFile.close()
    log('--- current version is [%s] ---' % (version))

    return True

def execute(cmd):
    exportStr = 'export'
    env = {}
    env['BUILD_ACTION'] = ACTION.lower()
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

def unixLink(script):
    # some file system do not support `ln`
    # so we create a new shell script
    # which redirect all input arguments to dest script
    f = open('latte', 'w')
    f.write('basepath=$(cd `dirname $0`; pwd)\n')
    f.write('$basepath/' + script + ' "$@"\n')
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

    if unixLink(script):
        return True
    else:
        log('Create shortcut script failed')
        return False

def performModule(module):
    log('--- Start to build module [%s] ---' % (module))
    return execute('cd %s\n%s clean latte%s' % (module, GRADLE_CMD, ACTION))

def perform():
    for m in BUILD_MODULES:
        if not performModule(m):
            return False
    return True

def start():
    if ACTION == 'Deploy':
        global DEPLOY_USER
        global DEPLOY_PASS
        DEPLOY_USER = raw_input('user: ')
        DEPLOY_PASS = getpass.getpass('pass: ')

    log('============================')
    log( ACTION + ' Start')
    log('============================')
    startTime = time.time()

    res = check() and perform()
    if ACTION == 'Build':
        scripts()

    endTime = time.time()
    if res:
        log('============================')
        log( ACTION + ' Successful')
        log('============================')
        log('Total time: %.3f secs' % (endTime - startTime))
    else:
        log('============================')
        log( ACTION + ' Failed')
        log('============================')

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
           [-a|--action %s]
           [-m|--modules module-name1,module-name2,...]''' % ('|'.join(ALLOWED_ACTIONS)))
        exit()
    try:
        extractArgs()
        if ACTION in ALLOWED_ACTIONS:
            ACTION = ACTION[0:1].upper() + ACTION[1:]
            start()
        else:
            log('Unknown action [' + ACTION + ']')
    except Exception, e:
        log(str(e))
