export default function Topbar() {
    const role = localStorage.getItem("role");

    return (
        <div className="flex items-center justify-between px-10 py-6 border-b">
            <h1 className="text-5xl font-extrabold text-black">
                Welcome, {role}
            </h1>


        <div className="w-14 h-14 rounded-full bg-indigo-200 flex items-center justify-center font-semibold text-black/70">
                        LADS
                    </div>
                </div>
            );
}