import { removeCategory } from "../services/categoriesService"

export default function CategoryList(props) {
    
    return <div>
        <table border={2}>
            <thead>
                <tr>
                    <td>Id</td>
                    <td>Name</td>
                    <td>x</td>

                </tr>
            </thead>
            <tbody>
                {props.categories.map((element) => {
                    return <tr key={element.id}>
                        <td>{element.id}</td>
                        <td>{element.name}</td>
                        <td><button onClick={async ()=>{
                            await removeCategory(element.id)
                            props.onDelete(element.id)
                            
                        }}>x</button></td>
                        

                    </tr>
                })}
            </tbody>
        </table>
    </div>
}