import React from 'react';

const EnvSecurityCard = () => {
  return (
    <div className="max-w-4xl mx-auto p-6 space-y-8">
      {/* 제목 섹션 */}
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold text-gray-800 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
          🔒 .env 파일 보안 가이드
        </h1>
        <p className="text-lg text-gray-600">
          환경변수 관리와 보안에 대한 완벽한 가이드
        </p>
      </div>

      {/* .env 파일 예시 섹션 */}
      <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-blue-500">
        <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center">
          📄 .env 파일 예시
        </h2>
        <p className="text-gray-600 mb-4">
          환경별로 나누어 관리하는 것이 핵심입니다.
        </p>
        
        <div className="grid md:grid-cols-3 gap-4">
          {/* 로컬 개발용 */}
          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 className="font-semibold text-green-700 mb-2">로컬 개발용 (.env.local)</h3>
            <pre className="text-sm bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
{`NEXT_PUBLIC_API_URL=http://localhost:8000
NEXT_PUBLIC_APP_NAME=Newsphere
SECRET_KEY=local-secret-key-123`}
            </pre>
          </div>

          {/* 개발 서버용 */}
          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 className="font-semibold text-yellow-700 mb-2">개발 서버용 (.env.development)</h3>
            <pre className="text-sm bg-gray-900 text-yellow-400 p-3 rounded overflow-x-auto">
{`NEXT_PUBLIC_API_URL=https://dev-api.example.com
NEXT_PUBLIC_APP_NAME=Newsphere (Dev)
SECRET_KEY=dev-secret-key-456`}
            </pre>
          </div>

          {/* 운영 서버용 */}
          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <h3 className="font-semibold text-red-700 mb-2">운영 서버용 (.env.production)</h3>
            <pre className="text-sm bg-gray-900 text-red-400 p-3 rounded overflow-x-auto">
{`NEXT_PUBLIC_API_URL=https://api.example.com
NEXT_PUBLIC_APP_NAME=Newsphere
SECRET_KEY=prod-secret-key-789`}
            </pre>
          </div>
        </div>
      </div>

      {/* GitHub에 올리면 안 되는 이유 섹션 */}
      <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-red-500">
        <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center">
          🚫 GitHub에 올리면 안 되는 이유
        </h2>
        <p className="text-gray-600 mb-4">
          .env 파일은 <strong className="text-red-600">민감 정보(Secrets)</strong>가 포함되어 있기 때문에 절대 외부에 공개되면 안 됩니다.
        </p>
        
        <div className="space-y-4">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h3 className="font-semibold text-red-800 mb-2">1. API Key & 비밀번호 유출</h3>
            <ul className="text-red-700 space-y-1 text-sm">
              <li>• 악의적인 사용자가 API 요청을 무단으로 수행할 수 있음</li>
              <li>• 클라우드, 데이터베이스, 결제 시스템까지 해킹 위험</li>
            </ul>
          </div>

          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h3 className="font-semibold text-red-800 mb-2">2. 운영 서버 공격 가능</h3>
            <ul className="text-red-700 space-y-1 text-sm">
              <li>• 서버 주소와 인증 키를 통해 직접 공격 시도 가능</li>
              <li>• CORS, 방화벽을 우회할 여지 생김</li>
            </ul>
          </div>

          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h3 className="font-semibold text-red-800 mb-2">3. 서비스 비용 폭탄</h3>
            <ul className="text-red-700 space-y-1 text-sm">
              <li>• 외부에서 무단으로 API 호출 → 과금 폭증</li>
              <li>• 특히 OpenAI, AWS, Google API 등은 치명적</li>
            </ul>
          </div>
        </div>
      </div>

      {/* GitHub에 올리지 않는 방법 섹션 */}
      <div className="bg-white rounded-xl shadow-lg p-6 border-l-4 border-green-500">
        <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center">
          🔒 GitHub에 올리지 않는 방법
        </h2>
        
        <div className="space-y-6">
          <div>
            <h3 className="font-semibold text-green-800 mb-2">1. .gitignore에 추가</h3>
            <pre className="text-sm bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
{`# 환경변수 파일
.env*`}
            </pre>
          </div>

          <div>
            <h3 className="font-semibold text-green-800 mb-2">2. GitHub에 이미 올라간 경우</h3>
            <pre className="text-sm bg-gray-900 text-green-400 p-3 rounded overflow-x-auto">
{`git rm --cached .env.local
git commit -m "[SECURITY] Remove .env.local from repository"
git push origin main`}
            </pre>
            <p className="text-sm text-gray-600 mt-2">
              → 그리고 반드시 해당 키를 폐기하고 새로 발급해야 합니다.
            </p>
          </div>
        </div>
      </div>

      {/* 정리 섹션 */}
      <div className="bg-gradient-to-r from-blue-500 to-purple-600 rounded-xl shadow-lg p-6 text-white">
        <h2 className="text-2xl font-bold mb-4 flex items-center">
          💡 정리
        </h2>
        <ul className="space-y-2 text-lg">
          <li>• .env는 환경변수 관리 전용 파일이자 민감정보 저장소</li>
          <li>• 절대 GitHub에 올리지 말고, .gitignore로 제외</li>
          <li>• 환경별로 .env를 분리하면 로컬/개발/운영 배포가 편해짐</li>
        </ul>
      </div>

      {/* 경고 배너 */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex items-center">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <p className="text-sm text-yellow-700">
              <strong>주의:</strong> 이 가이드는 Newsphere 프로젝트의 보안을 위한 것입니다. 
              실제 프로덕션 환경에서는 더욱 엄격한 보안 정책을 적용해야 합니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnvSecurityCard;
